package com.ashutosh.cyberhelper.repository;

import com.ashutosh.cyberhelper.entity.DocumentChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link DocumentChunk} — the per-chunk storage used in the RAG pipeline.
 *
 * <p>
 * <b>Current queries (Week 5):</b> existence check, list by document, delete by document.
 * </p>
 *
 * <p>
 * <b>Week 7 will add:</b> a native pgvector cosine similarity query:
 * <pre>
 *   SELECT * FROM document_chunks
 *    ORDER BY embedding <=> ?::vector
 *    LIMIT ?
 * </pre>
 * This cannot be expressed as a Spring Data derived query — it will be a
 * {@code @Query(nativeQuery = true)} when needed.
 * </p>
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * Checks whether any chunks have already been created for the given document.
     * Used by {@link com.ashutosh.cyberhelper.service.DocumentChunkingService}
     * to prevent re-chunking a document that has already been chunked.
     *
     * @param documentId the ID of the parent document
     * @return true if at least one chunk exists for this document
     */
    boolean existsByDocumentId(Long documentId);

    /**
     * Retrieves all chunks for a given document, paginated.
     * Ordered by chunkIndex so the caller receives chunks in document reading order.
     *
     * @param documentId the ID of the parent document
     * @param pageable   pagination and sorting parameters
     * @return a page of chunks for this document
     */
    Page<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId, Pageable pageable);

    /**
     * Returns the count of chunks stored for a document.
     * Used to populate {@code chunksCreated} in the response DTO.
     *
     * @param documentId the ID of the parent document
     * @return number of chunks stored
     */
    long countByDocumentId(Long documentId);

    /**
     * Deletes all chunks for a given document.
     * Reserved for future use — e.g., if an admin wants to re-chunk with
     * different chunk-size settings after modifying a document.
     *
     * @param documentId the ID of the parent document
     */
    void deleteByDocumentId(Long documentId);
}
