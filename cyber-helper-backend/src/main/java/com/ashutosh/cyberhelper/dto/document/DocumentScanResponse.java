package com.ashutosh.cyberhelper.dto.document;

import java.util.List;

/**
 * Response DTO for the {@code POST /documents/scan} endpoint.
 *
 * <p>
 * <b>Why this exists:</b> The scan operation discovers multiple files at once.
 * This DTO provides a summary of what happened during the scan — how many files
 * were found, how many were newly registered, and how many were already known.
 * </p>
 *
 * <p>
 * This gives the admin clear feedback on what the scan accomplished without
 * having to manually compare before/after states.
 * </p>
 */
public record DocumentScanResponse(
                int totalFilesFound,
                int newFilesRegistered,
                int alreadyRegistered,
                List<DocumentResponse> registeredDocuments) {
}
