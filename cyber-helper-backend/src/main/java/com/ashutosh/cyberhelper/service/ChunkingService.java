package com.ashutosh.cyberhelper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a cleaned document text into overlapping character-level chunks.
 *
 * <p>
 * <b>Why this service exists:</b> LLMs have a fixed context window. SEBI circulars
 * can be tens of thousands of characters. This service breaks that wall of text into
 * small, self-contained segments (chunks) that fit comfortably within Llama 3's
 * context window alongside the user's question.
 * </p>
 *
 * <p>
 * <b>Strategy — sliding window with overlap:</b>
 * </p>
 * <ul>
 * <li>We advance a window of {@code chunkSize} characters through the text.</li>
 * <li>Each step moves forward by {@code (chunkSize - overlap)} characters.</li>
 * <li>The overlap ensures that a sentence split across two chunk boundaries still
 *     appears fully in at least one chunk. This prevents the RAG system from
 *     missing context that lives exactly on a boundary.</li>
 * </ul>
 *
 * <p>
 * <b>Configurable via {@code application.yaml}:</b>
 * </p>
 * <pre>
 * app:
 *   chunking:
 *     chunk-size: 800   # characters per chunk
 *     overlap: 150      # overlap between consecutive chunks
 * </pre>
 *
 * <p>
 * <b>This service is deliberately dumb:</b> It knows nothing about embeddings,
 * documents, or databases. It takes a String in and returns a List of Strings out.
 * That purity makes it easy to unit test and easy to swap for a smarter strategy
 * (e.g., sentence-aware splitting) in the future.
 * </p>
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    /**
     * Target size of each chunk in characters.
     * At ~4 chars/token, 800 chars ≈ 200 tokens — well within nomic-embed-text's 2048-token limit.
     */
    private final int chunkSize;

    /**
     * Number of characters that consecutive chunks share.
     * Prevents information loss at chunk boundaries.
     */
    private final int overlap;

    public ChunkingService(
            @Value("${app.chunking.chunk-size:800}") int chunkSize,
            @Value("${app.chunking.overlap:150}") int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        log.info("ChunkingService initialised: chunkSize={}, overlap={}", chunkSize, overlap);
    }

    /**
     * Splits the given text into overlapping chunks.
     *
     * <p>
     * If the text is shorter than {@code chunkSize}, a single-element list is returned
     * containing the entire text. Empty or blank text returns an empty list.
     * </p>
     *
     * @param text the cleaned document text to split
     * @return ordered list of text chunks; never null
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            log.warn("ChunkingService.chunk() called with null or blank text — returning empty list");
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;

        if (step <= 0) {
            throw new IllegalStateException(
                    "Invalid chunking config: overlap (" + overlap +
                    ") must be less than chunkSize (" + chunkSize + ")");
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).strip();

            // Skip chunks that are entirely whitespace after stripping
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end == text.length()) {
                break; // reached the end of the text
            }

            start += step;
        }

        log.debug("Chunked text of {} chars into {} chunks (chunkSize={}, overlap={})",
                text.length(), chunks.size(), chunkSize, overlap);

        return chunks;
    }
}
