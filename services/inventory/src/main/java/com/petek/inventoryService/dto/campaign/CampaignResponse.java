package com.petek.inventoryService.dto.campaign;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.petek.inventoryService.dto.product.ProductResponse;
import com.petek.inventoryService.entity.Campaign.CampaignType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private Long campaignId;
    private String campaignName;
    private CampaignType campaignType;
    private BigDecimal discountPercentage;
    private Integer buyQty;
    private Integer getQty;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<ProductResponse> products;
    private Instant createdAt;
    private Instant updatedAt;
}
