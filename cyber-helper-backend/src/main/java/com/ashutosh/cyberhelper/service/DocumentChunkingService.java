package com.ashutosh.cyberhelper.service;

import com.ashutosh.cyberhelper.dto.document.ChunkingResultResponse;
import com.ashutosh.cyberhelper.entity.Document;
import com.ashutosh.cyberhelper.entity.DocumentChunk;
import com.ashutosh.cyberhelper.entity.DocumentStatus;
import com.ashutosh.cyberhelper.entity.ParsedDocument;
import com.ashutosh.cyberhelper.exception.DocumentNotFoundException;
import com.ashutosh.cyberhelper.exception.DocumentProcessingException;
import com.ashutosh.cyberhelper.repository.DocumentChunkRepository;
import com.ashutosh.cyberhelper.repository.DocumentRepository;
import com.ashutosh.cyberhelper.repository.ParsedDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the Week 5 chunking pipeline for a document.
 *
 * <p>
 * <b>Why this exists:</b> The chunking step is logically distinct from text
 * extraction (Week 4) and embedding generation (Week 6). Keeping it in its own
 * service respects the single-responsibility principle and makes each stage of
 * the RAG pipeline independently re-runnable — e.g., if chunk-size config
 * changes, only chunking needs to be re-run, not PDF extraction.
 * </p>
 *
 * <p>
 * <b>Pipeline orchestrated here:</b>
 * </p>
 * <ol>
 * <li>Load the {@link Document} — validate it is in {@code PROCESSED} status</li>
 * <li>Guard against re-chunking (idempotency check)</li>
 * <li>Load {@link ParsedDocument} — source of the cleaned text</li>
 * <li>Call {@link ChunkingService} to split text into chunks</li>
 * <li>Build and bulk-save {@link DocumentChunk} entities</li>
 * <li>Update document status to {@code CHUNKED}</li>
 * </ol>
 *
 * <p>
 * <b>What this service does NOT do:</b> It does not call Ollama. The
 * {@code embedding} field on each {@link DocumentChunk} is left {@code null}.
 * Week 6's {@code EmbeddingService} will fill that field.
 * </p>
 */
@Service
public class DocumentChunkingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkingService.class);

    private final DocumentRepository documentRepository;
    private final ParsedDocumentRepository parsedDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ChunkingService chunkingService;

    public DocumentChunkingService(
            DocumentRepository documentRepository,
            ParsedDocumentRepository parsedDocumentRepository,
            DocumentChunkRepository documentChunkRepository,
            ChunkingService chunkingService) {
        this.documentRepository = documentRepository;
        this.parsedDocumentRepository = parsedDocumentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.chunkingService = chunkingService;
    }

    /**
     * Chunks a single document by its ID.
     *
     * @param documentId the ID of the document to chunk
     * @return a summary of the chunking result
     * @throws DocumentNotFoundException   if the document does not exist
     * @throws DocumentProcessingException if the document is not in PROCESSED status,
     *                                     or if it has already been chunked,
     *                                     or if no parsed text is found
     */
    @Transactional
    public ChunkingResultResponse chunkDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + documentId));

        // Only PROCESSED documents can be chunked
        if (document.getStatus() != DocumentStatus.PROCESSED) {
            throw new DocumentProcessingException(
                    "Document must be in PROCESSED status to be chunked. " +
                    "Current status: " + document.getStatus());
        }

        // Idempotency guard — prevent double-chunking
        if (documentChunkRepository.existsByDocumentId(documentId)) {
            throw new DocumentProcessingException(
                    "Document has already been chunked. Document id: " + documentId +
                    ". Delete existing chunks first if you want to re-chunk.");
        }

        return executeChunking(document);
    }

    /**
     * Chunks all documents that are currently in {@code PROCESSED} status.
     *
     * <p>
     * Documents that have already been chunked are silently skipped.
     * Each document is processed in its own independent transaction via
     * {@link #executeChunking(Document)} — a failure on one document does not
     * affect the others.
     * </p>
     *
     * @return list of chunking results, one per eligible document
     */
    public List<ChunkingResultResponse> chunkAllProcessed() {
        // NOT @Transactional here — each document gets its own transaction inside executeChunking.
        // A single @Transactional here would mean one DB exception corrupts the session for
        // all subsequent documents in the loop.
        List<Document> processedDocs = documentRepository.findByStatus(DocumentStatus.PROCESSED);

        if (processedDocs.isEmpty()) {
            log.info("No documents found with status PROCESSED");
            return List.of();
        }

        log.info("Found {} PROCESSED documents to chunk", processedDocs.size());

        List<ChunkingResultResponse> results = new ArrayList<>();
        for (Document document : processedDocs) {
            // Skip already-chunked documents gracefully
            if (documentChunkRepository.existsByDocumentId(document.getId())) {
                log.info("Skipping already-chunked document: {} (id: {})",
                        document.getFileName(), document.getId());
                continue;
            }
            results.add(executeChunking(document));
        }

        return results;
    }

    // ─────────────────────────────────────────────────────────────
    // Private pipeline
    // ─────────────────────────────────────────────────────────────

    /**
     * Runs the full chunking pipeline for a single document in an isolated transaction.
     *
     * <p>
     * {@code REQUIRES_NEW} is critical here: it suspends any existing transaction and
     * opens a fresh one just for this document. If {@code saveAll} throws a DB exception
     * (e.g., type mismatch, constraint violation), only this document's transaction is
     * rolled back — the Hibernate session is cleanly closed before the catch block runs.
     * The catch block can then safely call {@code documentRepository.save()} because it
     * executes within the same fresh-but-still-open transaction before it commits or rolls back.
     * </p>
     *
     * <p>
     * This method is package-private (not private) so that Spring's CGLIB proxy can
     * intercept it and apply the {@code @Transactional} advice. Private methods are not
     * intercepted by Spring AOP.
     * </p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    ChunkingResultResponse executeChunking(Document document) {
        log.info("Starting chunking for document: {} (id: {})",
                document.getFileName(), document.getId());

        try {
            // Load the parsed text — this is what we split into chunks
            ParsedDocument parsedDocument = parsedDocumentRepository
                    .findByDocumentId(document.getId())
                    .orElseThrow(() -> new DocumentProcessingException(
                            "No parsed text found for document id: " + document.getId() +
                            ". Run document processing first."));

            String cleanedText = parsedDocument.getCleanedText();
            if (cleanedText == null || cleanedText.isBlank()) {
                throw new DocumentProcessingException(
                        "Cleaned text is empty for document id: " + document.getId());
            }

            // Split the text into overlapping chunks
            List<String> rawChunks = chunkingService.chunk(cleanedText);

            if (rawChunks.isEmpty()) {
                throw new DocumentProcessingException(
                        "Chunking produced zero chunks for document id: " + document.getId());
            }

            // Build DocumentChunk entities.
            // embedding is null — Week 6 will fill it.
            List<DocumentChunk> chunks = new ArrayList<>(rawChunks.size());
            for (int i = 0; i < rawChunks.size(); i++) {
                String chunkText = rawChunks.get(i);
                chunks.add(DocumentChunk.builder()
                        .document(document)
                        .chunkIndex(i)
                        .content(chunkText)
                        .tokenEstimate(chunkText.length() / 4)
                        .embedding(null)  // filled by Week 6 EmbeddingService
                        .build());
            }

            // Bulk-save all chunks in one transaction
            documentChunkRepository.saveAll(chunks);

            // Advance document status
            document.setStatus(DocumentStatus.CHUNKED);
            documentRepository.save(document);

            log.info("Successfully chunked document: {} (id: {}) into {} chunks",
                    document.getFileName(), document.getId(), chunks.size());

            return new ChunkingResultResponse(
                    document.getId(),
                    document.getFileName(),
                    chunks.size(),
                    DocumentStatus.CHUNKED.name(),
                    null);

        } catch (Exception e) {
            log.error("Failed to chunk document: {} (id: {})",
                    document.getFileName(), document.getId(), e);

            // Mark as FAILED so it can be retried after fixing the root cause
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);

            return new ChunkingResultResponse(
                    document.getId(),
                    document.getFileName(),
                    0,
                    DocumentStatus.FAILED.name(),
                    e.getMessage());
        }
    }
}
