package com.petek.inventoryService.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "current_stock")
public class CurrentStock {
    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "quantity_on_hand", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityReserved;

    @Column(name = "quantity_available", insertable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantityAvailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_movement_id")
    private StockMovement lastMovement;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
}
