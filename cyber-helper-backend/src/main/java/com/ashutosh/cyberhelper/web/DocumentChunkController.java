package com.ashutosh.cyberhelper.web;

import com.ashutosh.cyberhelper.dto.document.ChunkingResultResponse;
import com.ashutosh.cyberhelper.dto.document.DocumentChunkResponse;
import com.ashutosh.cyberhelper.entity.DocumentChunk;
import com.ashutosh.cyberhelper.service.DocumentChunkingService;
import com.ashutosh.cyberhelper.repository.DocumentChunkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for document chunking operations — Week 5 of the RAG pipeline.
 *
 * <p>
 * <b>Why this exists:</b> The admin needs to trigger chunking as a separate,
 * explicit step after text extraction (Week 4). This keeps each stage of the
 * pipeline independently re-runnable and observable.
 * </p>
 *
 * <p>
 * <b>Base path:</b> {@code /api/admin/documents}
 * </p>
 *
 * <p>
 * <b>Endpoints:</b>
 * </p>
 * <ul>
 * <li>{@code POST /{id}/chunk} — chunk a single document</li>
 * <li>{@code POST /chunk-all} — chunk all PROCESSED documents</li>
 * <li>{@code GET /{id}/chunks} — list chunks for a document (paginated)</li>
 * </ul>
 *
 * <p>
 * <b>Security:</b> All endpoints require the {@code ADMIN} authority,
 * matching the existing pattern in {@link DocumentController}.
 * </p>
 */
@RestController
@RequestMapping("/admin/documents")
public class DocumentChunkController {

    // Max characters to include in content preview when listing chunks.
    // Full content is available per-chunk; listing only needs a preview.
    private static final int CONTENT_PREVIEW_LENGTH = 200;

    private final DocumentChunkingService documentChunkingService;
    private final DocumentChunkRepository documentChunkRepository;

    public DocumentChunkController(
            DocumentChunkingService documentChunkingService,
            DocumentChunkRepository documentChunkRepository) {
        this.documentChunkingService = documentChunkingService;
        this.documentChunkRepository = documentChunkRepository;
    }

    /**
     * Chunks a single document that has been processed (text extracted).
     *
     * <p>
     * Precondition: document status must be {@code PROCESSED}.
     * Returns {@code 409 Conflict} (via GlobalExceptionHandler) if the document
     * has already been chunked.
     * </p>
     *
     * @param id the ID of the document to chunk
     * @return chunking result summary
     */
    @PostMapping("/{id}/chunk")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ChunkingResultResponse> chunkDocument(@PathVariable Long id) {
        ChunkingResultResponse result = documentChunkingService.chunkDocument(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Chunks all documents currently in {@code PROCESSED} status.
     *
     * <p>
     * Already-chunked documents are silently skipped.
     * If no documents are eligible, returns an empty list.
     * </p>
     *
     * @return list of chunking results, one per eligible document
     */
    @PostMapping("/chunk-all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<ChunkingResultResponse>> chunkAllProcessed() {
        List<ChunkingResultResponse> results = documentChunkingService.chunkAllProcessed();
        return ResponseEntity.ok(results);
    }

    /**
     * Lists chunks stored for a document, paginated, with content preview.
     *
     * <p>
     * Returns chunks ordered by {@code chunkIndex} (document reading order).
     * Content is truncated to {@value CONTENT_PREVIEW_LENGTH} characters in the
     * list view to keep response size manageable.
     * </p>
     *
     * @param id       the ID of the parent document
     * @param pageable pagination parameters (default page=0, size=20)
     * @return paginated list of chunk previews
     */
    @GetMapping("/{id}/chunks")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<DocumentChunkResponse>> getChunks(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<DocumentChunk> chunkPage = documentChunkRepository
                .findByDocumentIdOrderByChunkIndex(id, pageable);

        Page<DocumentChunkResponse> responsePage = chunkPage.map(chunk ->
                new DocumentChunkResponse(
                        chunk.getId(),
                        chunk.getDocument().getId(),
                        chunk.getChunkIndex(),
                        truncate(chunk.getContent(), CONTENT_PREVIEW_LENGTH),
                        chunk.getTokenEstimate(),
                        chunk.getEmbedding() != null));

        return ResponseEntity.ok(responsePage);
    }

    // ─────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
