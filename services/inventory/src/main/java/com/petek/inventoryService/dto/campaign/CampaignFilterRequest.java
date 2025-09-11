package com.petek.inventoryService.dto.campaign;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.petek.inventoryService.entity.Campaign.CampaignType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignFilterRequest {

    @NotNull
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @NotNull
    @Min(1)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private List<String> sort = List.of("campaignId");

    private String q;
    private CampaignType type;
    private LocalDate startGte;
    private LocalDate startLte;
    private LocalDate endGte;
    private LocalDate endLte;
    private LocalDate activeOn;
    private Instant updatedAfter;
}