package com.petek.planning.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

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
@Table(name = "plan_recommendations")
public class PlanRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;
    
    @Column(name = "forecast_id", nullable = false)
    private Long forecastId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;
    
    @Column(name = "horizon_days", nullable = false)
    @Builder.Default
    private Integer horizonDays = 7;
    
    @Column(name = "response_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode responseJson;
    
    @Column(name = "model", nullable = false, length = 50)
    @Builder.Default
    private String model = "gemma3:4b";
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
