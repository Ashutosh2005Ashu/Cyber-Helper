package com.ashutosh.cyberhelper.repository;

import com.ashutosh.cyberhelper.entity.Document;
import com.ashutosh.cyberhelper.entity.DocumentStatus;
import com.ashutosh.cyberhelper.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Document} entities.
 *
 * <p>
 * <b>Why this exists:</b> Abstracts all database access for documents.
 * The service layer uses these methods for scan-based duplicate detection,
 * status-based filtering, and type-based filtering.
 * </p>
 *
 * <p>
 * <b>Week 4 usage:</b> {@code DocumentProcessingService} will call
 * {@code findByStatus(READY_FOR_PROCESSING)} to discover documents that
 * need PDFBox text extraction.
 * </p>
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find a document by its stored filename.
     * Used during scan to detect duplicates.
     */
    Optional<Document> findByFileName(String fileName);

    /**
     * Check if a document with this filename already exists.
     * More efficient than findByFileName when we only need existence check.
     */
    boolean existsByFileName(String fileName);

    /**
     * Find all documents in a given processing status.
     * Week 4 will use this to find READY_FOR_PROCESSING documents.
     */
    List<Document> findByStatus(DocumentStatus status);

    /**
     * Find all documents of a given type.
     * Useful for filtering (e.g., show only SEBI circulars).
     */
    List<Document> findByDocumentType(DocumentType documentType);
}
