package com.petek.inventoryService.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "v_day_offer_stats", schema = "inv_forecast")
public class DayOfferStats {
    @Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "active_offers_count")
    private Integer activeOffersCount;

    @Column(name = "offer_avg_pct", precision = 5, scale = 2)
    private BigDecimal offerAvgPct;

    @Column(name = "offer_max_pct", precision = 5, scale = 2)
    private BigDecimal offerMaxPct;
}
