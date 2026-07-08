package com.ashutosh.cyberhelper.service.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Performs lightweight cleaning on extracted document text.
 *
 * <p>
 * <b>Why this exists:</b> PDFBox extraction often produces messy text with
 * extra whitespace, inconsistent line endings, and empty lines from page
 * breaks. This service normalizes the text without altering the legal
 * content.
 * </p>
 *
 * <p>
 * <b>What it does:</b>
 * </p>
 * <ul>
 * <li>Normalize line endings ({@code \r\n} → {@code \n})</li>
 * <li>Remove duplicate spaces within lines</li>
 * <li>Collapse 3+ consecutive empty lines into 2</li>
 * <li>Trim leading/trailing whitespace</li>
 * </ul>
 *
 * <p>
 * <b>What it does NOT do:</b> It does not rewrite, paraphrase, or
 * restructure legal text. The content remains faithful to the original.
 * </p>
 */
@Service
public class TextCleaningService {

    private static final Logger log = LoggerFactory.getLogger(TextCleaningService.class);

    /**
     * Cleans extracted text with lightweight normalization.
     *
     * @param rawText the raw text extracted from a document
     * @return cleaned text
     */
    public String clean(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        int originalLength = rawText.length();

        String cleaned = rawText;

        // 1. Normalize line endings: \r\n → \n, then standalone \r → \n
        cleaned = cleaned.replace("\r\n", "\n");
        cleaned = cleaned.replace("\r", "\n");

        // 2. Remove duplicate spaces within lines (but NOT newlines)
        //    Replace 2+ spaces/tabs with a single space
        cleaned = cleaned.replaceAll("[ \\t]{2,}", " ");

        // 3. Collapse 3+ consecutive newlines into 2 (keeps paragraph breaks)
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        // 4. Trim leading/trailing whitespace from each line
        cleaned = cleaned.lines()
                .map(String::strip)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // 5. Trim the entire result
        cleaned = cleaned.strip();

        log.debug("Cleaned text: {} chars → {} chars", originalLength, cleaned.length());

        return cleaned;
    }
}
