package com.ashutosh.cyberhelper.service.parser;

import com.ashutosh.cyberhelper.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads text content from plain text (.txt) files.
 *
 * <p>
 * <b>Why this exists:</b> Some compliance documents or advisories may be
 * provided as plain text files. This service reads them into a string
 * for the same downstream processing as PDF files.
 * </p>
 *
 * <p>
 * <b>Design note:</b> TXT files are always treated as a single "page".
 * The result uses the same {@link PdfParserService.ParseResult} record
 * for consistency across the pipeline.
 * </p>
 */
@Service
public class TxtParserService {

    private static final Logger log = LoggerFactory.getLogger(TxtParserService.class);

    /**
     * Reads the complete text content of a TXT file.
     *
     * @param filePath absolute path to the TXT file on disk
     * @return parse result containing file text and page count of 1
     * @throws DocumentProcessingException if the file cannot be read
     */
    public PdfParserService.ParseResult parse(String filePath) {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new DocumentProcessingException("TXT file not found: " + filePath);
        }

        log.info("Reading TXT file: {}", path.getFileName());

        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);

            if (text == null || text.isBlank()) {
                throw new DocumentProcessingException("TXT file is empty: " + filePath);
            }

            log.info("Read {} characters from TXT file: {}",
                    text.length(), path.getFileName());

            return new PdfParserService.ParseResult(text, 1);

        } catch (IOException e) {
            throw new DocumentProcessingException(
                    "Failed to read TXT file: " + path.getFileName(), e);
        }
    }
}
