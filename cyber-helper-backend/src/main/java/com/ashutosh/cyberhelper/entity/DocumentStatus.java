package com.ashutosh.cyberhelper.entity;

/**
 * Tracks a document's position in the processing pipeline.
 *
 * <p>
 * <b>Why this exists:</b> The RAG pipeline is multi-stage. A document goes
 * through:
 * upload → registration → text extraction → chunking → embedding. This enum
 * lets each
 * stage know which documents are ready for it.
 * </p>
 *
 * <p>
 * <b>Current statuses (Week 3):</b>
 * </p>
 * <ul>
 * <li>{@code UPLOADED} — file exists on disk, just discovered by scan</li>
 * <li>{@code REGISTERED} — metadata saved in PostgreSQL</li>
 * <li>{@code READY_FOR_PROCESSING} — admin confirmed, ready for Week 4's PDFBox
 * extraction</li>
 * </ul>
 *
 * <p>
 * <b>Week 4 will add:</b> {@code PROCESSING}, {@code PROCESSED}, {@code FAILED}
 * </p>
 */
public enum DocumentStatus {

    UPLOADED,
    REGISTERED,
    READY_FOR_PROCESSING
}
