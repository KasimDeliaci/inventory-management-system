package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.campaign.CampaignCreateRequest;
import com.petek.inventoryService.dto.campaign.CampaignResponse;
import com.petek.inventoryService.entity.Campaign;

@Service
public class CampaignMapper {

    private ProductMapper productMapper = new ProductMapper();

    /**
     * Map CampaignCreateRequest to Campaign entity.
     */
    public Campaign toCampaign(CampaignCreateRequest request) {
        return Campaign.builder()
            .campaignName(request.getCampaignName())
            .campaignType(request.getCampaignType())
            .discountPercentage(request.getDiscountPercentage())
            .buyQty(request.getBuyQty())
            .getQty(request.getBuyQty())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .build();
    }

    /**
     * Map
     */
    public CampaignResponse toCampaignResponse(Campaign campaign) {
        return CampaignResponse.builder()
            .campaignId(campaign.getCampaignId())
            .campaignName(campaign.getCampaignName())
            .campaignType(campaign.getCampaignType())
            .discountPercentage(campaign.getDiscountPercentage())
            .buyQty(campaign.getBuyQty())
            .getQty(campaign.getGetQty())
            .startDate(campaign.getStartDate())
            .endDate(campaign.getEndDate())
            .products(campaign.getProducts() == null ? null : campaign.getProducts().stream().map(productMapper::toProductResponse).toList())
            .createdAt(campaign.getCreatedAt())
            .updatedAt(campaign.getUpdatedAt())
            .build();
    }
    
}
