package com.petek.inventoryService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET deleted_at = NOW() WHERE product_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "product_name")
    private String productName;
    
    private String description;
    private String category;
    
    @Column(name = "unit_of_measure")
    private String unitOfMeasure;
    
    @Column(name = "safety_stock")
    private BigDecimal safetyStock;
    
    @Column(name = "reorder_point") 
    private BigDecimal reorderPoint;
    
    @Column(name = "current_price")
    private BigDecimal currentPrice;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
