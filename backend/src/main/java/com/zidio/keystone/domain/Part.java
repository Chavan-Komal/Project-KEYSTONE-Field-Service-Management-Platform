package com.zidio.keystone.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "parts")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "stock_qty", nullable = false)
    private int stockQty;

    public Part() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Part p = new Part();
        public Builder name(String name) { p.name = name; return this; }
        public Builder sku(String sku) { p.sku = sku; return this; }
        public Builder unitCost(BigDecimal unitCost) { p.unitCost = unitCost; return this; }
        public Builder stockQty(int stockQty) { p.stockQty = stockQty; return this; }
        public Part build() { return p; }
    }
}
