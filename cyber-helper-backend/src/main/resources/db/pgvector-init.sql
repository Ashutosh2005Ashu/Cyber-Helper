-- Enable the pgvector extension.
-- This is the ONLY statement here intentionally — Spring Boot's ScriptUtils splits scripts
-- on semicolons, which destroys multi-statement DO $$ ... $$ blocks.
-- The implicit varchar->vector cast is applied programmatically by PgVectorInitializer.java.
CREATE EXTENSION IF NOT EXISTS vector;
