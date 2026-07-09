package com.ashutosh.cyberhelper.entity;

/**
 * Tracks a document's position in the processing pipeline.
 *
 * <p>
 * <b>Why this exists:</b> The RAG pipeline is multi-stage. A document goes
 * through:
 * upload → registration → text extraction → chunking → embedding. This enum
 * lets each stage know which documents are ready for it.
 * </p>
 *
 * <p>
 * <b>Status progression:</b>
 * </p>
 * <ol>
 * <li>{@code UPLOADED} — file exists on disk, just discovered by scan</li>
 * <li>{@code REGISTERED} — metadata saved in PostgreSQL</li>
 * <li>{@code READY_FOR_PROCESSING} — admin confirmed, ready for PDFBox extraction</li>
 * <li>{@code PROCESSING} — text extraction in progress</li>
 * <li>{@code PROCESSED} — text extracted and cleaned, ready for chunking</li>
 * <li>{@code CHUNKED} — text split into overlapping chunks stored in PostgreSQL (Week 5)</li>
 * <li>{@code EMBEDDED} — pgvector embeddings stored for all chunks (Week 6)</li>
 * <li>{@code FAILED} — a pipeline stage failed (see processingError on Document)</li>
 * </ol>
 */
public enum DocumentStatus {

    UPLOADED,
    REGISTERED,
    READY_FOR_PROCESSING,

    /** Week 4: Text extraction is currently in progress. */
    PROCESSING,

    /** Week 4: Text extraction completed successfully. Ready for Week 5 chunking. */
    PROCESSED,

    /** Week 4: Text extraction failed (see processingError on Document). */
    FAILED,

    /** Week 5: Text has been split into overlapping chunks stored in document_chunks table. */
    CHUNKED,

    /**
     * Week 6: pgvector embeddings have been generated and stored for all chunks.
     * This document is now fully indexed and available for RAG similarity search.
     */
    EMBEDDED
}
