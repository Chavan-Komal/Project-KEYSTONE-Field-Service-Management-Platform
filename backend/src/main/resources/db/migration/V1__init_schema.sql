-- Project KEYSTONE — initial schema (MySQL 8.0.16+ required for CHECK constraint enforcement)
-- Section 05 (Domain Model) of the engineering brief.
--
-- IDs are CHAR(36) storing UUID strings. Hibernate generates the UUID values
-- in the application (see @GeneratedValue on each entity's @Id) — MySQL has
-- no built-in gen_random_uuid()-style default, so this is the simplest
-- cross-version-compatible approach.

CREATE TABLE customers (
    id             CHAR(36) PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    contact_email  VARCHAR(200) NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sites (
    id             CHAR(36) PRIMARY KEY,
    customer_id    CHAR(36) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    address        VARCHAR(300) NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sites_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_sites_customer ON sites(customer_id);

CREATE TABLE users (
    id             CHAR(36) PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    email          VARCHAR(200) NOT NULL UNIQUE,
    password_hash  VARCHAR(200) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('DISPATCHER','TECHNICIAN','MANAGER','CUSTOMER')),
    -- only populated for CUSTOMER-role users: which org they belong to
    customer_id    CHAR(36),
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_users_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE parts (
    id             CHAR(36) PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    sku            VARCHAR(80)  NOT NULL UNIQUE,
    unit_cost      DECIMAL(10,2) NOT NULL DEFAULT 0,
    stock_qty      INT NOT NULL DEFAULT 0 CHECK (stock_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    sla_due_at     DATETIME(6) NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_wo_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_wo_site FOREIGN KEY (site_id) REFERENCES sites(id),
    CONSTRAINT fk_wo_assigned FOREIGN KEY (assigned_to) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_wo_customer ON work_orders(customer_id);
CREATE INDEX idx_wo_site ON work_orders(site_id);
CREATE INDEX idx_wo_assigned ON work_orders(assigned_to);
CREATE INDEX idx_wo_status ON work_orders(status);

-- Append-only audit trail — rows are never updated or deleted (Section 05, integrity rules).
CREATE TABLE work_order_status_history (
    id             CHAR(36) PRIMARY KEY,
    work_order_id  CHAR(36) NOT NULL,
    from_status    VARCHAR(20),
    to_status      VARCHAR(20) NOT NULL,
    changed_by     CHAR(36) NOT NULL,
    changed_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    note           VARCHAR(500),
    CONSTRAINT fk_history_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_history_wo ON work_order_status_history(work_order_id);

CREATE TABLE part_usage (
    id                CHAR(36) PRIMARY KEY,
    work_order_id     CHAR(36) NOT NULL,
    part_id           CHAR(36) NOT NULL,
    qty_used          INT NOT NULL CHECK (qty_used > 0),
    unit_cost_at_use  DECIMAL(10,2) NOT NULL,
    logged_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_partusage_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_partusage_part FOREIGN KEY (part_id) REFERENCES parts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_partusage_wo ON part_usage(work_order_id);

CREATE TABLE time_logs (
    id             CHAR(36) PRIMARY KEY,
    work_order_id  CHAR(36) NOT NULL,
    technician_id  CHAR(36) NOT NULL,
    minutes        INT NOT NULL CHECK (minutes > 0),
    note           VARCHAR(500),
    logged_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_timelog_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_timelog_user FOREIGN KEY (technician_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_timelog_wo ON time_logs(work_order_id);
