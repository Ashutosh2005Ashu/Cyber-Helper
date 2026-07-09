package com.ashutosh.cyberhelper.entity;

import com.ashutosh.cyberhelper.config.FloatArrayConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * Represents a single text chunk extracted from a {@link ParsedDocument}.
 *
 * <p>
 * <b>Why this exists:</b> LLMs have a limited context window. We cannot pass an
 * entire SEBI circular (potentially hundreds of pages) to Llama 3. Instead, we
 * split the cleaned text into overlapping chunks, each small enough to fit in the
 * context window alongside the user's question. This entity is the storage record
 * for one such chunk.
 * </p>
 *
 * <p>
 * <b>Chunking strategy (Week 5):</b> Simple sliding-window character chunking.
 * Each chunk is {@code app.chunking.chunk-size} characters with
 * {@code app.chunking.overlap} character overlap between consecutive chunks.
 * </p>
 *
 * <p>
 * <b>How it fits into the RAG architecture:</b>
 * </p>
 * <ul>
 * <li>Week 5 (now): {@code DocumentChunkingService} creates and saves these chunks.</li>
 * <li>Week 6: {@code EmbeddingService} calls Ollama nomic-embed-text for each chunk
 *     and stores the resulting 768-dimensional vector in the {@code embedding} field.</li>
 * <li>Week 7: RAG service issues a pgvector cosine-similarity query
 *     ({@code embedding <=> ?::vector}) to retrieve the top-k most relevant chunks
 *     for a user's question.</li>
 * </ul>
 *
 * <p>
 * <b>The {@code embedding} column:</b> Declared as {@code vector(768)} in PostgreSQL.
 * JPA cannot natively handle this type, so a {@link FloatArrayConverter} translates
 * between {@code float[]} and pgvector's text format ({@code [0.1,0.2,...]}).
 * The value is {@code null} until Week 6's embedding step runs.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent document.
     * Used by the RAG service to trace retrieved chunks back to the source
     * document for generating citations (e.g., "SEBI Circular No. XYZ, Clause 3").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    /**
     * Zero-based position of this chunk within the document.
     * Together with documentId, uniquely identifies a chunk's position.
     * Also used to reconstruct the reading order of chunks during citation.
     */
    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    /**
     * The actual text content of this chunk.
     * This is what gets sent to the LLM as context alongside the user's question.
     * Stored as TEXT (no length limit) because pgvector similarity search
     * operates on the embedding column, not this one.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Rough estimate of the token count for this chunk.
     * Computed as {@code content.length() / 4} — a common heuristic for English text.
     * Stored so Week 7 can quickly filter out chunks that would exceed the LLM's
     * context window without having to re-calculate.
     */
    @Column(name = "token_estimate", nullable = false)
    private int tokenEstimate;

    /**
     * The 768-dimensional embedding vector produced by Ollama's nomic-embed-text model.
     *
     * <p>
     * Declared as {@code vector(768)} in PostgreSQL — this is the pgvector column type.
     * The {@link FloatArrayConverter} handles serialisation/deserialisation.
     * </p>
     *
     * <p>
     * This field is {@code null} when first created (Week 5). It is populated in
     * Week 6 by {@code EmbeddingService}. Similarity search becomes possible only
     * after this field is filled for all chunks of a document.
     * </p>
     */
    @Convert(converter = FloatArrayConverter.class)
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
