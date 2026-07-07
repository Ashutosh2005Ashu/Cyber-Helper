package com.ashutosh.cyberhelper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Central metadata record for every document in the knowledge base.
 *
 * <p>
 * <b>Why this exists:</b> The RAG pipeline needs to know what files exist,
 * where they
 * are stored, and what stage of processing they are in. This entity is the
 * single source
 * of truth for document lifecycle management.
 * </p>
 *
 * <p>
 * <b>How it fits into the RAG architecture:</b>
 * </p>
 * <ul>
 * <li>Week 3 (now): Stores file metadata after filesystem scan</li>
 * <li>Week 4: {@code DocumentProcessingService} queries this entity to find
 * files
 * with status {@code READY_FOR_PROCESSING}, extracts text via PDFBox, and may
 * store the raw text in a related entity</li>
 * <li>Week 5: Chunking service reads extracted text linked to this
 * document</li>
 * <li>Week 7: RAG service traces retrieved chunks back to this entity for
 * citations</li>
 * </ul>
 *
 * <p>
 * <b>Relationships:</b>
 * </p>
 * <ul>
 * <li>{@code uploadedBy} → {@link User} (nullable, since filesystem scan has no
 * user context)</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, unique = true, length = 500)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 500)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DocumentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
