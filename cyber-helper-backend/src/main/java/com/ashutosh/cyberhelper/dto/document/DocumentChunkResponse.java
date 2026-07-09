package com.ashutosh.cyberhelper.dto.document;

/**
 * Response DTO representing a single document chunk.
 *
 * <p>
 * <b>Why this exists:</b> The {@link com.ashutosh.cyberhelper.entity.DocumentChunk}
 * entity holds the full chunk content plus a potentially large embedding float array.
 * The REST response must never expose raw embedding data — it is meaningless to the
 * caller and would bloat the response payload significantly.
 * </p>
 *
 * <p>
 * The {@code content} field is returned in full for individual chunk retrieval
 * but can be truncated at the controller level for list endpoints where the caller
 * only needs a preview.
 * </p>
 *
 * @param chunkId        database ID of this chunk
 * @param documentId     ID of the parent document
 * @param chunkIndex     zero-based position of this chunk within the document
 * @param content        the chunk text (may be truncated for list views)
 * @param tokenEstimate  rough token count (content.length / 4)
 * @param embedded       true if the pgvector embedding has been generated for this chunk
 */
public record DocumentChunkResponse(
        Long chunkId,
        Long documentId,
        int chunkIndex,
        String content,
        int tokenEstimate,
        boolean embedded
) {
}
