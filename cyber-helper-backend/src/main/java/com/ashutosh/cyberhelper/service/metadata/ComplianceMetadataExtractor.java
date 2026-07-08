package com.ashutosh.cyberhelper.service.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort extraction of SEBI compliance metadata from document text.
 *
 * <p>
 * <b>Why this exists:</b> SEBI circulars follow semi-structured formats.
 * This service uses regex patterns to extract commonly occurring fields
 * like circular numbers, dates, and clause references. This metadata
 * helps in organizing and filtering documents in the knowledge base.
 * </p>
 *
 * <p>
 * <b>Design note:</b> All extraction is best-effort. If a pattern is not
 * found, the corresponding field returns {@code null}. The AI pipeline
 * in later weeks can provide more accurate extraction — this is a
 * pre-processing step for basic organization.
 * </p>
 */
@Service
public class ComplianceMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(ComplianceMetadataExtractor.class);

    /**
     * Maximum number of characters to scan for metadata.
     * Circular metadata is almost always in the first few pages.
     */
    private static final int SCAN_LIMIT = 3000;

    // ─── Regex patterns for SEBI circular metadata ─────────────

    /**
     * Matches SEBI circular numbers like:
     * - SEBI/HO/MRD/TPD/P/CIR/2023/165
     * - SEBI/HO/MIRSD/MIRSD-PoD-1/P/CIR/2024/12
     * - Circular No. CIR/MRD/DP/13/2013
     */
    private static final Pattern CIRCULAR_NUMBER_PATTERN = Pattern.compile(
            "(?:SEBI/HO/[A-Z0-9/\\-]+(?:/CIR/\\d{4}/\\d+)?)" +
                    "|(?:(?:Circular\\s*(?:No\\.?|Number)\\s*[:\\-]?\\s*)([A-Z0-9/\\-]+))",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Matches dates in common SEBI circular formats:
     * - Month DD, YYYY (e.g., "January 15, 2024")
     * - DD/MM/YYYY or DD-MM-YYYY
     * - YYYY-MM-DD
     */
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?:(?:January|February|March|April|May|June|July|August|September|October|November|December)" +
                    "\\s+\\d{1,2},?\\s+\\d{4})" +
                    "|(?:\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{4})" +
                    "|(?:\\d{4}[/\\-]\\d{1,2}[/\\-]\\d{1,2})",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Matches the subject/title line in SEBI circulars.
     * Typically: "Subject: ..." or "Sub: ..." or "Re: ..."
     */
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "(?:Subject|Sub|Re)\\s*[:\\-]\\s*(.+?)(?:\\n|$)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Matches clause, section, paragraph, and regulation references:
     * - Clause 5.1, Clause 3(a)
     * - Section 12, Section 3(1)(a)
     * - Para 4.2, Paragraph 7
     * - Regulation 31(2)
     */
    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
            "(?:Clause|Section|Para(?:graph)?|Regulation|Rule|Annexure|Schedule)\\s+" +
                    "\\d+(?:[.(]\\d+[).]?)*(?:\\([a-zA-Z]\\))?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Holds extracted compliance metadata.
     */
    public record ComplianceMetadata(
            String circularNumber,
            String circularDate,
            String circularTitle,
            String clauseReferences) {
    }

    /**
     * Extracts compliance metadata from document text.
     * Scans only the first {@value SCAN_LIMIT} characters for efficiency.
     *
     * @param cleanedText the cleaned document text
     * @return extracted metadata (fields may be null if not found)
     */
    public ComplianceMetadata extract(String cleanedText) {
        if (cleanedText == null || cleanedText.isBlank()) {
            log.debug("Empty text provided, returning empty metadata");
            return new ComplianceMetadata(null, null, null, null);
        }

        // Only scan the beginning of the document for metadata
        String scanText = cleanedText.substring(0, Math.min(cleanedText.length(), SCAN_LIMIT));

        String circularNumber = extractCircularNumber(scanText);
        String circularDate = extractDate(scanText);
        String circularTitle = extractTitle(scanText);
        // Clause references can appear anywhere, scan full text
        String clauseReferences = extractClauseReferences(cleanedText);

        log.info("Metadata extracted — circular: {}, date: {}, title: {}, clauses: {}",
                circularNumber != null ? "found" : "not found",
                circularDate != null ? "found" : "not found",
                circularTitle != null ? "found" : "not found",
                clauseReferences != null ? "found" : "not found");

        return new ComplianceMetadata(circularNumber, circularDate, circularTitle, clauseReferences);
    }

    // ─── Private extraction methods ─────────────────────────────

    private String extractCircularNumber(String text) {
        Matcher matcher = CIRCULAR_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            // Group 0 is the full match; group 1 is the captured number after "Circular No."
            String result = matcher.group(1) != null ? matcher.group(1) : matcher.group(0);
            return result.strip();
        }
        return null;
    }

    private String extractDate(String text) {
        // Look for "Date:" or "Dated:" line first
        Pattern datedPattern = Pattern.compile(
                "(?:Date[d]?\\s*[:\\-]\\s*)(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE);
        Matcher datedMatcher = datedPattern.matcher(text);
        if (datedMatcher.find()) {
            return datedMatcher.group(1).strip();
        }

        // Fall back to any date pattern in the text
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).strip();
        }
        return null;
    }

    private String extractTitle(String text) {
        Matcher matcher = TITLE_PATTERN.matcher(text);
        if (matcher.find()) {
            String title = matcher.group(1).strip();
            // Limit title length to avoid capturing too much
            if (title.length() > 500) {
                title = title.substring(0, 500);
            }
            return title;
        }
        return null;
    }

    private String extractClauseReferences(String text) {
        Set<String> references = new LinkedHashSet<>();
        Matcher matcher = CLAUSE_PATTERN.matcher(text);

        while (matcher.find() && references.size() < 50) {
            references.add(matcher.group(0).strip());
        }

        if (references.isEmpty()) {
            return null;
        }

        String result = String.join(", ", references);
        // Respect the column length limit (2000 chars)
        if (result.length() > 1900) {
            result = result.substring(0, 1900);
            int lastComma = result.lastIndexOf(", ");
            if (lastComma > 0) {
                result = result.substring(0, lastComma);
            }
        }

        return result;
    }
}
