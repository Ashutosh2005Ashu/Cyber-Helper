package com.ashutosh.cyberhelper.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that maps a Java {@code float[]} to and from
 * the pgvector text representation used by the {@code vector} column type.
 *
 * <p>
 * <b>Why this exists:</b> PostgreSQL's pgvector extension stores vectors as a
 * custom type (e.g. {@code vector(768)}). JPA/Hibernate has no built-in mapping
 * for it. This converter handles the translation so the rest of the code works
 * with a plain {@code float[]} — the natural Java representation of an embedding.
 * </p>
 *
 * <p>
 * <b>pgvector text format:</b> {@code [0.1,0.2,0.3,...]} — square brackets,
 * comma-separated floats, no spaces.
 * </p>
 *
 * <p>
 * <b>How it is used:</b> Applied via {@code @Convert(converter = FloatArrayConverter.class)}
 * on the {@code embedding} field of {@link com.ashutosh.cyberhelper.entity.DocumentChunk}.
 * </p>
 *
 * <p>
 * <b>Week 6 note:</b> When Ollama's {@code nomic-embed-text} generates a 768-dimensional
 * embedding vector, the EmbeddingService will set this field and call save(). This
 * converter will then serialise it automatically before the INSERT/UPDATE.
 * </p>
 */
@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    /**
     * Converts a Java {@code float[]} to pgvector's text representation.
     *
     * <p>Example: {@code [0.1,0.2,0.3]}</p>
     *
     * @param embedding the embedding vector, may be null (chunk not yet embedded)
     * @return pgvector-formatted string, or null if embedding is null
     */
    @Override
    public String convertToDatabaseColumn(float[] embedding) {
        if (embedding == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Converts pgvector's text representation back to a Java {@code float[]}.
     *
     * <p>Handles both formats returned by the JDBC driver:
     * {@code [0.1,0.2]} and {@code {0.1,0.2}}</p>
     *
     * @param dbData the raw string from the database, may be null
     * @return Java float array, or null if dbData is null/blank
     */
    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        // Strip surrounding brackets: [ ] or { }
        String stripped = dbData.trim()
                .replaceAll("^[\\[{]", "")
                .replaceAll("[\\]}]$", "");

        if (stripped.isBlank()) {
            return new float[0];
        }

        String[] parts = stripped.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
