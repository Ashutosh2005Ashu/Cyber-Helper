package com.ashutosh.cyberhelper.dto.document;

import java.time.LocalDateTime;

/**
 * Response DTO returned after document processing (text extraction).
 *
 * <p>
 * Includes the processing outcome: status, extracted metadata,
 * and any error message if processing failed.
 * </p>
 */
public record DocumentProcessingResponse(
        Long documentId,
        String fileName,
        String status,
        Integer pageCount,
        String circularNumber,
        String circularTitle,
        String circularDate,
        String clauseReferences,
        LocalDateTime processedAt,
        String errorMessage) {
}
