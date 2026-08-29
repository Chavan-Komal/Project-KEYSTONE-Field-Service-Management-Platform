-- Project KEYSTONE — initial schema (PostgreSQL)
-- Section 05 (Domain Model) of the engineering brief.
--
-- IDs are CHAR(36) storing UUID strings. Hibernate generates the UUID values
-- in the application (see @GeneratedValue on each entity's @Id) — kept as
-- CHAR(36) rather than the native UUID type so the JDBC mapping
-- (preferred_uuid_jdbc_type: CHAR) stays identical across environments.

CREATE TABLE customers (
    id             CHAR(36) PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    contact_email  VARCHAR(200) NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sites (
    id             CHAR(36) PRIMARY KEY,
    customer_id    CHAR(36) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    address        VARCHAR(300) NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sites_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);
CREATE INDEX idx_sites_customer ON sites(customer_id);

CREATE TABLE users (
    id             CHAR(36) PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    email          VARCHAR(200) NOT NULL UNIQUE,
    password_hash  VARCHAR(200) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('DISPATCHER','TECHNICIAN','MANAGER','CUSTOMER')),
    -- only populated for CUSTOMER-role users: which org they belong to
    customer_id    CHAR(36),
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE parts (
    id             CHAR(36) PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    sku            VARCHAR(80)  NOT NULL UNIQUE,
    unit_cost      DECIMAL(10,2) NOT NULL DEFAULT 0,
    stock_qty      INT NOT NULL DEFAULT 0 CHECK (stock_qty >= 0)
);

CREATE TABLE work_orders (
    id             CHAR(36) PRIMARY KEY,
    code           VARCHAR(20) NOT NULL UNIQUE,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    priority       VARCHAR(20) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status         VARCHAR(20) NOT NULL CHECK (status IN
                     ('NEW','ASSIGNED','IN_PROGRESS','ON_HOLD','COMPLETED','CLOSED','CANCELLED')),
    customer_id    CHAR(36) NOT NULL,
    site_id        CHAR(36) NOT NULL,
    assigned_to    CHAR(36),
    sla_due_at     TIMESTAMP(6) NOT NULL,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wo_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_wo_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_wo_assigned FOREIGN KEY (assigned_to) REFERENCES users(id)
);
CREATE INDEX idx_wo_customer ON work_orders(customer_id);
CREATE INDEX idx_wo_site ON work_orders(site_id);
CREATE INDEX idx_wo_assigned ON work_orders(assigned_to);
CREATE INDEX idx_wo_status ON work_orders(status);

-- PostgreSQL has no "ON UPDATE CURRENT_TIMESTAMP" column clause (unlike MySQL) —
-- a trigger is the standard way to keep updated_at current on every row change.
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_work_orders_updated_at
    BEFORE UPDATE ON work_orders
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- Append-only audit trail — rows are never updated or deleted (Section 05, integrity rules).
CREATE TABLE work_order_status_history (
    id             CHAR(36) PRIMARY KEY,
    work_order_id  CHAR(36) NOT NULL,
    from_status    VARCHAR(20),
    to_status      VARCHAR(20) NOT NULL,
    changed_by     CHAR(36) NOT NULL,
    changed_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note           VARCHAR(500),
    CONSTRAINT fk_history_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by) REFERENCES users(id)
);
CREATE INDEX idx_history_wo ON work_order_status_history(work_order_id);

CREATE TABLE part_usage (
    id                CHAR(36) PRIMARY KEY,
    work_order_id     CHAR(36) NOT NULL,
    part_id           CHAR(36) NOT NULL,
    qty_used          INT NOT NULL CHECK (qty_used > 0),
    unit_cost_at_use  DECIMAL(10,2) NOT NULL,
    logged_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_partusage_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_partusage_part FOREIGN KEY (part_id) REFERENCES parts(id)
);
CREATE INDEX idx_partusage_wo ON part_usage(work_order_id);

CREATE TABLE time_logs (
    id             CHAR(36) PRIMARY KEY,
    work_order_id  CHAR(36) NOT NULL,
    technician_id  CHAR(36) NOT NULL,
    minutes        INT NOT NULL CHECK (minutes > 0),
    note           VARCHAR(500),
    logged_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_timelog_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_timelog_user FOREIGN KEY (technician_id) REFERENCES users(id)
);
CREATE INDEX idx_timelog_wo ON time_logs(work_order_id);
