package com.ashutosh.cyberhelper.exception;

/**
 * Thrown when document text extraction or processing fails.
 *
 * <p>
 * Examples: corrupt PDF file, empty file, PDFBox extraction error,
 * unsupported file format.
 * </p>
 */
public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
