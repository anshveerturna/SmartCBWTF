package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Consumable Order Item - Line item in a consumable order.
 * Pricing is captured at order time and immutable.
 */
@Entity
@Table(name = "consumable_order_item", indexes = {
        @Index(name = "idx_consumable_order_item_order", columnList = "order_id")
})
public class ConsumableOrderItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private ConsumableOrder order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "consumable_item_id")
    private ConsumableItem consumableItem;

    // Captured at order time (immutable)
    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_per_unit", precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePerUnit;

    @Column(name = "gst_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal gstRate;

    // Calculated
    @Column(name = "line_subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineSubtotal;

    @Column(name = "line_gst", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineGst;

    @Column(name = "line_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConsumableOrder getOrder() {
        return order;
    }

    public void setOrder(ConsumableOrder order) {
        this.order = order;
    }

    public ConsumableItem getConsumableItem() {
        return consumableItem;
    }

    public void setConsumableItem(ConsumableItem consumableItem) {
        this.consumableItem = consumableItem;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public void setLineSubtotal(BigDecimal lineSubtotal) {
        this.lineSubtotal = lineSubtotal;
    }

    public BigDecimal getLineGst() {
        return lineGst;
    }

    public void setLineGst(BigDecimal lineGst) {
        this.lineGst = lineGst;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    // Calculate line totals
    public void calculateTotals() {
        this.lineSubtotal = pricePerUnit.multiply(BigDecimal.valueOf(quantity));
        this.lineGst = lineSubtotal.multiply(gstRate).divide(BigDecimal.valueOf(100));
        this.lineTotal = lineSubtotal.add(lineGst);
    }
}
