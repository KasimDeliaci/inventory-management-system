package com.petek.inventoryService.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.campaign.CampaignCreateRequest;
import com.petek.inventoryService.dto.campaign.CampaignResponse;
import com.petek.inventoryService.entity.Campaign;
import com.petek.inventoryService.entity.Campaign.CampaignType;
import com.petek.inventoryService.mapper.CampaignMapper;
import com.petek.inventoryService.repository.CampaignRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CampaignService {
    
    private final CampaignRepository repository;
    private final CampaignMapper mapper;

    /**
     * Create campaign.
     */
    public CampaignResponse createCampaign(CampaignCreateRequest request) {
        Campaign campaign = mapper.toCampaign(request);
        
        if (campaign.getCampaignType() == CampaignType.BXGY_SAME_PRODUCT) {
            if (campaign.getBuyQty() == null || campaign.getGetQty() == null) {
                throw new IllegalArgumentException("Buy quantity and get quantity must not be null for BXGY_SAME_PRODUCT campaign type");
            }
            campaign.setDiscountPercentage(null);
        } else {
            if (campaign.getDiscountPercentage() == null) {
                throw new IllegalArgumentException("Discount percentage must not be null for DISCOUNT campaign type");
            }
            campaign.setBuyQty(null);
            campaign.setGetQty(null);
        }

        campaign.setCreatedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());

        return mapper.toCampaignResponse(repository.save(campaign));
    }

    /**
     * Get campaign by id.
     */
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long campaignId) {
        return repository.findById(campaignId)
            .map(mapper::toCampaignResponse)
            .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
    }

}
