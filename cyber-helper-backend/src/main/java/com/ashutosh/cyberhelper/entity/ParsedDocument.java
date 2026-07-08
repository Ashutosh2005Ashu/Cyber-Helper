package com.ashutosh.cyberhelper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores the extracted and cleaned text content of a processed document.
 *
 * <p>
 * <b>Why a separate table?</b> PDF text can be hundreds of kilobytes per document.
 * Keeping it in the {@link Document} table would bloat every query that only needs
 * metadata. This table is read on-demand — primarily by Week 5's chunking service.
 * </p>
 *
 * <p>
 * <b>How it fits into the RAG architecture:</b>
 * </p>
 * <ul>
 * <li>Week 4 (now): {@code DocumentProcessingService} creates this after text extraction</li>
 * <li>Week 5: Chunking service reads {@code cleanedText} to split into chunks</li>
 * <li>Future: If chunking strategy changes, re-chunk from stored text without re-parsing PDFs</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parsed_documents")
public class ParsedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the parent document. Each document has at most one parsed result.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    /**
     * Full text extracted from the file, before any cleaning.
     * Stored so we can re-clean with different strategies if needed.
     */
    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    /**
     * Cleaned version of the extracted text.
     * This is what Week 5's chunking service will consume.
     */
    @Column(name = "cleaned_text", columnDefinition = "TEXT")
    private String cleanedText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
