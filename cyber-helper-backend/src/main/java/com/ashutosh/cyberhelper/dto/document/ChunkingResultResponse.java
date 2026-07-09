package com.ashutosh.cyberhelper.dto.document;

/**
 * Response DTO returned after a chunking operation completes for a document.
 *
 * <p>
 * <b>Why this exists:</b> The admin needs to know whether the chunking succeeded,
 * how many chunks were created, and what the document's new status is — all in a
 * single response, without having to query the document endpoint separately.
 * </p>
 *
 * @param documentId    the ID of the document that was chunked
 * @param documentName  the file name of the document (for easy identification)
 * @param chunksCreated the number of chunks inserted into {@code document_chunks}
 * @param status        the new {@link com.ashutosh.cyberhelper.entity.DocumentStatus}
 *                      as a string — should be "CHUNKED" on success, "FAILED" on error
 * @param errorMessage  non-null only when status is "FAILED"
 */
public record ChunkingResultResponse(
        Long documentId,
        String documentName,
        int chunksCreated,
        String status,
        String errorMessage
) {
}
