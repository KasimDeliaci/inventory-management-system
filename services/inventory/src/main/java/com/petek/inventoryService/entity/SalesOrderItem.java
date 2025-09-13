package com.petek.inventoryService.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "sales_order_items")
public class SalesOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_item_id")
    private Long salesOrderItemId;
    
    @Column(name = "sales_order_id", nullable = false)
    private Long salesOrderId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "discount_amount", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal discountAmount;
    
    @Column(name = "line_total", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal lineTotal;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}