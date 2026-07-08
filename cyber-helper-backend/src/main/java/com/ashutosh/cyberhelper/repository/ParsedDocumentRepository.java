package com.ashutosh.cyberhelper.repository;

import com.ashutosh.cyberhelper.entity.ParsedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ParsedDocument} entities.
 *
 * <p>
 * <b>Why this exists:</b> Provides database access for stored extracted text.
 * </p>
 *
 * <p>
 * <b>Week 5 usage:</b> The chunking service will call
 * {@code findByDocumentId()} to load the cleaned text for splitting.
 * </p>
 */
@Repository
public interface ParsedDocumentRepository extends JpaRepository<ParsedDocument, Long> {

    /**
     * Find the parsed text for a specific document.
     * Used by Week 5's chunking service.
     */
    Optional<ParsedDocument> findByDocumentId(Long documentId);

    /**
     * Check if a document has already been parsed.
     * Prevents duplicate processing.
     */
    boolean existsByDocumentId(Long documentId);
}
