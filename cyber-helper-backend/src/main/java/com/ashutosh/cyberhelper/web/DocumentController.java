package com.ashutosh.cyberhelper.web;

import com.ashutosh.cyberhelper.dto.document.DocumentProcessingResponse;
import com.ashutosh.cyberhelper.dto.document.DocumentResponse;
import com.ashutosh.cyberhelper.dto.document.DocumentScanResponse;
import com.ashutosh.cyberhelper.entity.DocumentStatus;
import com.ashutosh.cyberhelper.entity.DocumentType;
import com.ashutosh.cyberhelper.service.DocumentProcessingService;
import com.ashutosh.cyberhelper.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for document management.
 *
 * <p>
 * <b>Why this exists:</b> Provides the admin with HTTP endpoints to manage the
 * document knowledge base. The admin copies PDF files to the filesystem, then
 * uses
 * these endpoints to register, inspect, and manage them.
 * </p>
 *
 * <p>
 * <b>How it fits into the RAG architecture:</b> This controller is the admin's
 * interface for preparing documents before the RAG pipeline processes them. The
 * flow is:
 * <ol>
 * <li>Admin copies PDFs to {@code storage/uploads/sebi/}</li>
 * <li>Admin calls {@code POST /documents/scan} to register them</li>
 * <li>Admin calls {@code PATCH /documents/{id}/status} to mark them as
 * {@code READY_FOR_PROCESSING}</li>
 * <li>Admin calls {@code POST /documents/{id}/process} to trigger text
 * extraction</li>
 * </ol>
 *
 * <p>
 * <b>Security:</b> All endpoints require ADMIN authority, enforced via
 * {@code @PreAuthorize}. Non-admin users receive 403 Forbidden.
 * </p>
 */
@RestController
@RequestMapping("/documents")
@PreAuthorize("hasAuthority('ADMIN')")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;

    public DocumentController(
            DocumentService documentService,
            DocumentProcessingService documentProcessingService) {
        this.documentService = documentService;
        this.documentProcessingService = documentProcessingService;
    }

    /**
     * Scans the upload directory and registers any new PDF files in the database.
     * Skips files that are already registered (duplicate detection by fileName).
     *
     * @return summary of scan results including newly registered documents
     */
    @PostMapping("/scan")
    public ResponseEntity<DocumentScanResponse> scanDocuments() {
        DocumentScanResponse response = documentService.scanAndRegisterDocuments();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Returns all registered documents.
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    /**
     * Returns a single document by its database ID.
     *
     * @param id document ID
     * @return document metadata
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    /**
     * Returns all documents with the given processing status.
     *
     * @param status one of UPLOADED, REGISTERED, READY_FOR_PROCESSING, PROCESSING,
     *               PROCESSED, FAILED
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByStatus(
            @PathVariable DocumentStatus status) {
        return ResponseEntity.ok(documentService.getDocumentsByStatus(status));
    }

    /**
     * Returns all documents of the given type.
     *
     * @param type one of SEBI_CIRCULAR, CSCRF, MASTER_CIRCULAR, CERT_IN_ADVISORY,
     *             ORGANIZATION_POLICY, OTHER
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByType(
            @PathVariable DocumentType type) {
        return ResponseEntity.ok(documentService.getDocumentsByType(type));
    }

    /**
     * Updates a document's processing status.
     * Typical usage: mark a document as READY_FOR_PROCESSING after review.
     *
     * @param id     document ID
     * @param status new status value
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<DocumentResponse> updateDocumentStatus(
            @PathVariable Long id,
            @RequestParam DocumentStatus status) {
        return ResponseEntity.ok(documentService.updateDocumentStatus(id, status));
    }

    /**
     * Bulk updates all REGISTERED documents to READY_FOR_PROCESSING.
     *
     * <p>
     * Use this once to prepare already-registered documents for the Week 4
     * parsing pipeline. New documents registered after this change are
     * automatically set to READY_FOR_PROCESSING at scan time.
     * </p>
     */
    @PostMapping("/status/ready-all")
    public ResponseEntity<List<DocumentResponse>> bulkReadyAll() {
        return ResponseEntity.ok(documentService.updateAllRegisteredToReady());
    }

    /**
     * Deletes a document's metadata from the database.
     * By default, only the database record is removed.
     * Pass {@code deleteFile=true} to also delete the physical file from disk.
     *
     * @param id         document ID
     * @param deleteFile whether to also delete the physical PDF file (default:
     *                   false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean deleteFile) {
        documentService.deleteDocument(id, deleteFile);
        return ResponseEntity.noContent().build();
    }

    // ─── Week 4: Document Processing Endpoints ─────────────────

    /**
     * Triggers text extraction and metadata parsing for a single document.
     * The document must have status {@code READY_FOR_PROCESSING}.
     *
     * <p>
     * Pipeline: Parse (PDFBox/TXT) → Clean → Extract Metadata → Store
     * </p>
     *
     * @param id document ID
     * @return processing result with extracted metadata
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<DocumentProcessingResponse> processDocument(@PathVariable Long id) {
        DocumentProcessingResponse response = documentProcessingService.processDocument(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Triggers text extraction for all documents with status
     * {@code READY_FOR_PROCESSING}.
     *
     * @return list of processing results (one per document)
     */
    @PostMapping("/process-all")
    public ResponseEntity<List<DocumentProcessingResponse>> processAllDocuments() {
        List<DocumentProcessingResponse> responses = documentProcessingService.processAllReady();
        return ResponseEntity.ok(responses);
    }
}
