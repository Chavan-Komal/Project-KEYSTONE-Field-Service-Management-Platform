-- Project KEYSTONE — V3
-- Adds: (1) self-service password reset, (2) photo attachments on work orders.
--
-- IDs stay CHAR(36) storing UUID strings, consistent with V1.

-- ---------------------------------------------------------------
-- 1. Password reset tokens
-- ---------------------------------------------------------------
-- A short-lived, single-use token issued by POST /api/auth/forgot-password
-- and consumed by POST /api/auth/reset-password. Old/used rows are kept for
-- the audit trail rather than deleted.
CREATE TABLE password_reset_tokens (
    id          CHAR(36) PRIMARY KEY,
    user_id     CHAR(36) NOT NULL,
    token       VARCHAR(120) NOT NULL UNIQUE,
    expires_at  TIMESTAMP(6) NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_prt_token ON password_reset_tokens(token);
CREATE INDEX idx_prt_user ON password_reset_tokens(user_id);

-- ---------------------------------------------------------------
-- 2. Work order attachments (customer-supplied photos of the issue)
-- ---------------------------------------------------------------
-- Image bytes are stored inline as BYTEA — self-contained, no external object
-- store to provision. Capped at 5 MB per file by the multipart config; only
-- image/* content types are accepted at the service layer.
CREATE TABLE work_order_attachments (
    id             CHAR(36) PRIMARY KEY,
    work_order_id  CHAR(36) NOT NULL,
    filename       VARCHAR(255) NOT NULL,
    content_type   VARCHAR(100) NOT NULL,
    size_bytes     BIGINT NOT NULL,
    data           BYTEA NOT NULL,
    uploaded_by    CHAR(36) NOT NULL,
    uploaded_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_woa_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_woa_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
);
CREATE INDEX idx_woa_wo ON work_order_attachments(work_order_id);
