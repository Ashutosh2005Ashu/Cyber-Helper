package com.ashutosh.cyberhelper.dto.document;

import com.ashutosh.cyberhelper.entity.Document;

import java.time.LocalDateTime;

/**
 * Response DTO for document metadata.
 *
 * <p>
 * <b>Why this exists:</b> Exposes document metadata without leaking the JPA
 * entity.
 * Follows the same record-based DTO pattern as
 * {@link com.ashutosh.cyberhelper.dto.organization.OrganizationResponse}.
 * </p>
 *
 * <p>
 * <b>Week 4 usage:</b> After text extraction, the response may be extended
 * (or a new DTO created) to include extraction status details.
 * </p>
 */
public record DocumentResponse(
        Long id,
        String fileName,
        String originalFileName,
        String documentType,
        String status,
        String filePath,
        Long fileSize,
        String uploadedByEmail,
        LocalDateTime uploadDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /**
     * Factory method to convert a {@link Document} entity to this DTO.
     * Safely handles nullable {@code uploadedBy} relationship.
     */
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getOriginalFileName(),
                doc.getDocumentType().name(),
                doc.getStatus().name(),
                doc.getFilePath(),
                doc.getFileSize(),
                doc.getUploadedBy() != null ? doc.getUploadedBy().getEmail() : null,
                doc.getUploadDate(),
                doc.getCreatedAt(),
                doc.getUpdatedAt());
    }
}
