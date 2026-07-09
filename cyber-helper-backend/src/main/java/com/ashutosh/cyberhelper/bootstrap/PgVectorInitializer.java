package com.ashutosh.cyberhelper.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the pgvector extension is configured correctly on every startup.
 *
 * <p>
 * <b>Why this exists (not in pgvector-init.sql):</b>
 * Spring Boot's {@code ScriptUtils} splits SQL scripts by semicolons ({@code ;}).
 * A PL/pgSQL {@code DO $$ ... $$;} block contains multiple internal semicolons,
 * which {@code ScriptUtils} incorrectly treats as statement separators — producing
 * broken fragments that fail with "Unterminated dollar quote".
 * </p>
 *
 * <p>
 * <b>What this does:</b> Creates an IMPLICIT cast from {@code varchar} to {@code vector}
 * if one does not already exist. pgvector's default cast is only {@code ASSIGNMENT}, which
 * is not enough for JDBC prepared statements. Without {@code IMPLICIT}, Hibernate's
 * {@code VARCHAR}-typed null parameter binding causes:
 * <pre>
 *   ERROR: column "embedding" is of type vector but expression is of type character varying
 * </pre>
 * </p>
 *
 * <p>
 * <b>Timing:</b> {@link ApplicationRunner} runs after the Spring context is fully
 * initialised (after Hibernate DDL). The cast is needed for DML (INSERT/UPDATE),
 * not for DDL, so this timing is correct.
 * </p>
 *
 * <p>
 * <b>Idempotency:</b> Checks {@code pg_cast} before acting. Safe to run on every restart.
 * </p>
 */
@Component
public class PgVectorInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PgVectorInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public PgVectorInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureImplicitVarcharToVectorCast();
    }

    /**
     * Creates an IMPLICIT cast from {@code varchar} to {@code vector} if absent.
     *
     * <p>
     * If pgvector's ASSIGNMENT cast exists, it is dropped first, because PostgreSQL
     * does not support {@code ALTER CAST} — the only way to upgrade from ASSIGNMENT
     * to IMPLICIT is to drop and recreate.
     * </p>
     */
    private void ensureImplicitVarcharToVectorCast() {
        boolean hasImplicit = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (" +
                "  SELECT 1 FROM pg_cast c" +
                "  JOIN pg_type s ON c.castsource = s.oid" +
                "  JOIN pg_type t ON c.casttarget = t.oid" +
                "  WHERE s.typname = 'varchar' AND t.typname = 'vector'" +
                "  AND c.castcontext = 'i'" +
                ")",
                Boolean.class));

        if (hasImplicit) {
            log.debug("pgvector: IMPLICIT cast varchar->vector already exists — no action needed");
            return;
        }

        log.info("pgvector: IMPLICIT cast varchar->vector not found — creating it");

        boolean hasAny = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (" +
                "  SELECT 1 FROM pg_cast c" +
                "  JOIN pg_type s ON c.castsource = s.oid" +
                "  JOIN pg_type t ON c.casttarget = t.oid" +
                "  WHERE s.typname = 'varchar' AND t.typname = 'vector'" +
                ")",
                Boolean.class));

        if (hasAny) {
            // Drop the existing ASSIGNMENT cast before upgrading to IMPLICIT
            log.info("pgvector: Dropping ASSIGNMENT cast varchar->vector to replace with IMPLICIT");
            jdbcTemplate.execute("DROP CAST (varchar AS vector)");
        }

        // WITH INOUT: PostgreSQL uses varchar's output function (varchar_out → cstring)
        // and passes it to vector's input function (vector_in). This works because pgvector
        // accepts vectors in text format: [0.1,0.2,...,0.768]
        jdbcTemplate.execute("CREATE CAST (varchar AS vector) WITH INOUT AS IMPLICIT");
        log.info("pgvector: IMPLICIT cast varchar->vector created successfully");
    }
}
