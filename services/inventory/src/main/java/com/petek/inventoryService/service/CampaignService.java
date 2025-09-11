package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.campaign.CampaignCreateRequest;
import com.petek.inventoryService.dto.campaign.CampaignResponse;
import com.petek.inventoryService.dto.campaign.CampaignUpdateRequest;
import com.petek.inventoryService.entity.Campaign;
import com.petek.inventoryService.entity.Campaign.CampaignType;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.mapper.CampaignMapper;
import com.petek.inventoryService.repository.CampaignRepository;
import com.petek.inventoryService.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CampaignService {
    
    private final CampaignRepository repository;
    private final CampaignMapper mapper;

    private final ProductRepository productRepository;

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

    /**
     * Update campaign by id.
     */
    public CampaignResponse updateCampaign(Long campaignId, CampaignUpdateRequest request) {
        Campaign existingCampaign = repository.findById(campaignId)
            .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));

        if (existingCampaign.getCampaignType() == CampaignType.BXGY_SAME_PRODUCT) {
            Optional.ofNullable(request.getBuyQty())
                .ifPresent(existingCampaign::setBuyQty);

            Optional.ofNullable(request.getGetQty())
                .ifPresent(existingCampaign::setGetQty);
        } else {
            Optional.ofNullable(request.getDiscountPercentage())
                .ifPresent(existingCampaign::setDiscountPercentage);
        }

        Optional.ofNullable(request.getCampaignName())
            .filter(unit -> !unit.trim().isEmpty())
            .ifPresent(existingCampaign::setCampaignName);
        
        Optional.ofNullable(request.getStartDate())
            .ifPresent(existingCampaign::setStartDate);
        
        Optional.ofNullable(request.getEndDate())
            .ifPresent(existingCampaign::setEndDate);
        
        existingCampaign.setUpdatedAt(Instant.now());

        return mapper.toCampaignResponse(repository.save(existingCampaign));
    }

    /**
     * Delete campaign by id.
     */
    public void deleteCampaign(Long campaignId) {
        Campaign campaign = repository.findById(campaignId)
            .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
        repository.delete(campaign);
    }

    /**
     * Create Campaign Product.
     */
    public void assignCampaignProduct(Long campaignId, List<Long> productIds) {
        Campaign campaign = repository.findById(campaignId)
            .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
        
        List<Product> products = productRepository.findAllById(productIds);

        products.forEach(campaign::addProduct);

        campaign.setUpdatedAt(Instant.now());
        repository.save(campaign);
    }

    /**
     * Delete Campaign Product.
     */
    public void deleteCampaignProduct(Long campaignId, Long productId) {
        Campaign campaign = repository.findById(campaignId)
            .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

        if (!campaign.getProducts().contains(product)) {
            throw new EntityNotFoundException("Product not found with id: " + productId + " in Campaign with id: " + campaignId);
        }

        campaign.removeProduct(product);

        campaign.setUpdatedAt(Instant.now());
        repository.save(campaign);
    }

}
