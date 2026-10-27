CREATE TABLE sensitive_word (
    id UNIQUEIDENTIFIER PRIMARY KEY,
    term_value NVARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sensitive_word_value UNIQUE (term_value)
);
