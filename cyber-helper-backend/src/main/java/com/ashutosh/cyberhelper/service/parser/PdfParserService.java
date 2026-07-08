package com.ashutosh.cyberhelper.service.parser;

import com.ashutosh.cyberhelper.exception.DocumentProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extracts text content from PDF files using Apache PDFBox.
 *
 * <p>
 * <b>Why this exists:</b> SEBI circulars and CSCRF documents are distributed
 * as PDFs. This service reads each PDF from disk and extracts all text content
 * along with the page count.
 * </p>
 *
 * <p>
 * <b>How it works:</b>
 * </p>
 * <ol>
 * <li>Loads the PDF file using PDFBox's {@link Loader}</li>
 * <li>Uses {@link PDFTextStripper} to extract text from all pages</li>
 * <li>Returns the raw text and page count as a {@link ParseResult}</li>
 * </ol>
 *
 * <p>
 * <b>Design note:</b> PDFBox 3.x uses {@code Loader.loadPDF()} instead of the
 * deprecated {@code PDDocument.load()} from PDFBox 2.x.
 * </p>
 */
@Service
public class PdfParserService {

    private static final Logger log = LoggerFactory.getLogger(PdfParserService.class);

    /**
     * Holds the result of parsing a document file.
     *
     * @param text      full extracted text
     * @param pageCount number of pages (1 for TXT files)
     */
    public record ParseResult(String text, int pageCount) {
    }

    /**
     * Extracts all text and page count from a PDF file.
     *
     * @param filePath absolute path to the PDF file on disk
     * @return parse result containing raw text and page count
     * @throws DocumentProcessingException if the file cannot be read or parsed
     */
    public ParseResult parse(String filePath) {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new DocumentProcessingException("PDF file not found: " + filePath);
        }

        log.info("Parsing PDF: {}", path.getFileName());

        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            int pageCount = document.getNumberOfPages();

            if (pageCount == 0) {
                throw new DocumentProcessingException("PDF has no pages: " + filePath);
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.isBlank()) {
                throw new DocumentProcessingException(
                        "No text could be extracted from PDF (may be image-based): " + filePath);
            }

            log.info("Extracted {} characters from {} pages: {}",
                    text.length(), pageCount, path.getFileName());

            return new ParseResult(text, pageCount);

        } catch (IOException e) {
            throw new DocumentProcessingException(
                    "Failed to parse PDF: " + path.getFileName(), e);
        }
    }
}
