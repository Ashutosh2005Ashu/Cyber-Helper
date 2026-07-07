package com.ashutosh.cyberhelper.entity;

/**
 * Categorizes documents in the knowledge base.
 *
 * <p>
 * <b>Why this exists:</b> Different document types carry different regulatory
 * weight.
 * In Weeks 5–7, the RAG pipeline may prioritize certain document types during
 * retrieval
 * (e.g., SEBI circulars over internal policies).
 * </p>
 *
 * <p>
 * <b>Week 4 usage:</b> DocumentProcessingService may apply different text
 * extraction
 * strategies based on document type (e.g., table-heavy CSCRF docs vs.
 * text-heavy circulars).
 * </p>
 */
public enum DocumentType {

    SEBI_CIRCULAR,
    CSCRF,
    MASTER_CIRCULAR,
    CERT_IN_ADVISORY,
    ORGANIZATION_POLICY,
    OTHER
}
