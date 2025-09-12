package com.petek.inventoryService.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
@IdClass(ReportingId.class)
@Table(name = "v_product_day_promo", schema = "inv_forecast")
public class ProductDayPromo {
    @Id
    @Column(name = "date")
    private LocalDate date;

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "promo_pct", precision = 5, scale = 2)
    private BigDecimal promoPct;
}
