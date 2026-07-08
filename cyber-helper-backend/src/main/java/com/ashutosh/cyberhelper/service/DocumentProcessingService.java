package com.ashutosh.cyberhelper.service;

import com.ashutosh.cyberhelper.dto.document.DocumentProcessingResponse;
import com.ashutosh.cyberhelper.entity.Document;
import com.ashutosh.cyberhelper.entity.DocumentStatus;
import com.ashutosh.cyberhelper.entity.ParsedDocument;
import com.ashutosh.cyberhelper.exception.DocumentNotFoundException;
import com.ashutosh.cyberhelper.exception.DocumentProcessingException;
import com.ashutosh.cyberhelper.repository.DocumentRepository;
import com.ashutosh.cyberhelper.repository.ParsedDocumentRepository;
import com.ashutosh.cyberhelper.service.metadata.ComplianceMetadataExtractor;
import com.ashutosh.cyberhelper.service.parser.PdfParserService;
import com.ashutosh.cyberhelper.service.parser.TxtParserService;
import com.ashutosh.cyberhelper.service.text.TextCleaningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the complete document processing pipeline.
 *
 * <p>
 * <b>Why this exists:</b> This is the central coordinator for Week 4's
 * document parsing pipeline. It ties together the parser, cleaner, and
 * metadata extractor services into a single processing flow.
 * </p>
 *
 * <p>
 * <b>Pipeline flow:</b>
 * </p>
 * <ol>
 * <li>Load document from DB, validate status is {@code READY_FOR_PROCESSING}</li>
 * <li>Set status to {@code PROCESSING}</li>
 * <li>Call the appropriate parser (PDF or TXT based on file extension)</li>
 * <li>Call {@link TextCleaningService} on extracted raw text</li>
 * <li>Call {@link ComplianceMetadataExtractor} on cleaned text</li>
 * <li>Save {@link ParsedDocument} with raw + cleaned text</li>
 * <li>Update {@link Document} with metadata + set status to {@code PROCESSED}</li>
 * <li>On failure: set status to {@code FAILED} with error message</li>
 * </ol>
 *
 * <p>
 * <b>Week 5 will:</b> Read from {@code parsed_documents.cleaned_text} to
 * split into chunks for the vector database.
 * </p>
 */
@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentRepository documentRepository;
    private final ParsedDocumentRepository parsedDocumentRepository;
    private final PdfParserService pdfParserService;
    private final TxtParserService txtParserService;
    private final TextCleaningService textCleaningService;
    private final ComplianceMetadataExtractor metadataExtractor;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            ParsedDocumentRepository parsedDocumentRepository,
            PdfParserService pdfParserService,
            TxtParserService txtParserService,
            TextCleaningService textCleaningService,
            ComplianceMetadataExtractor metadataExtractor) {
        this.documentRepository = documentRepository;
        this.parsedDocumentRepository = parsedDocumentRepository;
        this.pdfParserService = pdfParserService;
        this.txtParserService = txtParserService;
        this.textCleaningService = textCleaningService;
        this.metadataExtractor = metadataExtractor;
    }

    /**
     * Processes a single document: extract text, clean, extract metadata, store.
     *
     * @param documentId the ID of the document to process
     * @return processing result with extracted metadata
     * @throws DocumentNotFoundException    if document doesn't exist
     * @throws DocumentProcessingException  if document is not in READY_FOR_PROCESSING status
     */
    @Transactional
    public DocumentProcessingResponse processDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + documentId));

        // Validate status
        if (document.getStatus() != DocumentStatus.READY_FOR_PROCESSING) {
            throw new DocumentProcessingException(
                    "Document is not ready for processing. Current status: " + document.getStatus()
                            + ". Mark it as READY_FOR_PROCESSING first.");
        }

        // Check if already parsed
        if (parsedDocumentRepository.existsByDocumentId(documentId)) {
            throw new DocumentProcessingException(
                    "Document has already been parsed. Document id: " + documentId);
        }

        return executeProcessing(document);
    }

    /**
     * Processes all documents with status {@code READY_FOR_PROCESSING}.
     *
     * @return list of processing results (one per document)
     */
    @Transactional
    public List<DocumentProcessingResponse> processAllReady() {
        List<Document> readyDocuments = documentRepository.findByStatus(
                DocumentStatus.READY_FOR_PROCESSING);

        if (readyDocuments.isEmpty()) {
            log.info("No documents found with status READY_FOR_PROCESSING");
            return List.of();
        }

        log.info("Found {} documents ready for processing", readyDocuments.size());

        List<DocumentProcessingResponse> results = new ArrayList<>();
        for (Document document : readyDocuments) {
            // Skip already-parsed documents
            if (parsedDocumentRepository.existsByDocumentId(document.getId())) {
                log.info("Skipping already-parsed document: {}", document.getFileName());
                continue;
            }
            results.add(executeProcessing(document));
        }

        return results;
    }

    // ─────────────────────────────────────────────────────────────
    // Private pipeline
    // ─────────────────────────────────────────────────────────────

    private DocumentProcessingResponse executeProcessing(Document document) {
        log.info("Starting processing for document: {} (id: {})",
                document.getFileName(), document.getId());

        // Mark as PROCESSING
        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        try {
            // Step 1: Parse — extract raw text based on file type
            PdfParserService.ParseResult parseResult = parseByFileType(document);

            // Step 2: Clean the extracted text
            String cleanedText = textCleaningService.clean(parseResult.text());

            // Step 3: Extract compliance metadata
            ComplianceMetadataExtractor.ComplianceMetadata metadata =
                    metadataExtractor.extract(cleanedText);

            // Step 4: Store parsed text
            ParsedDocument parsedDocument = ParsedDocument.builder()
                    .document(document)
                    .rawText(parseResult.text())
                    .cleanedText(cleanedText)
                    .build();
            parsedDocumentRepository.save(parsedDocument);

            // Step 5: Update document with metadata and status
            document.setPageCount(parseResult.pageCount());
            document.setCircularNumber(metadata.circularNumber());
            document.setCircularTitle(metadata.circularTitle());
            document.setCircularDate(metadata.circularDate());
            document.setClauseReferences(metadata.clauseReferences());
            document.setProcessingError(null);
            document.setProcessedAt(LocalDateTime.now());
            document.setStatus(DocumentStatus.PROCESSED);
            documentRepository.save(document);

            log.info("Successfully processed document: {} (id: {})",
                    document.getFileName(), document.getId());

            return toResponse(document);

        } catch (Exception e) {
            log.error("Failed to process document: {} (id: {})",
                    document.getFileName(), document.getId(), e);

            // Mark as FAILED with error message
            document.setStatus(DocumentStatus.FAILED);
            document.setProcessingError(e.getMessage());
            document.setProcessedAt(LocalDateTime.now());
            documentRepository.save(document);

            return toResponse(document);
        }
    }

    /**
     * Routes to the correct parser based on file extension.
     */
    private PdfParserService.ParseResult parseByFileType(Document document) {
        String fileName = document.getFileName().toLowerCase();

        if (fileName.endsWith(".pdf")) {
            return pdfParserService.parse(document.getFilePath());
        } else if (fileName.endsWith(".txt")) {
            return txtParserService.parse(document.getFilePath());
        } else {
            throw new DocumentProcessingException(
                    "Unsupported file type: " + fileName + ". Only PDF and TXT are supported.");
        }
    }

    /**
     * Maps a Document entity to the processing response DTO.
     */
    private DocumentProcessingResponse toResponse(Document document) {
        return new DocumentProcessingResponse(
                document.getId(),
                document.getFileName(),
                document.getStatus().name(),
                document.getPageCount(),
                document.getCircularNumber(),
                document.getCircularTitle(),
                document.getCircularDate(),
                document.getClauseReferences(),
                document.getProcessedAt(),
                document.getProcessingError());
    }
}
