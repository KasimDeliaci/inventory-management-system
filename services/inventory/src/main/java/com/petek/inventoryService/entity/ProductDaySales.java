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
@Table(name = "v_product_day_sales", schema = "inv_forecast")
public class ProductDaySales {
    @Id
    @Column(name = "date")
    private LocalDate date;

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "sales_units")
    private BigDecimal salesUnits;

    @Column(name = "offer_active_share", precision = 5, scale = 2)
    private BigDecimal offerActiveShare;
}
