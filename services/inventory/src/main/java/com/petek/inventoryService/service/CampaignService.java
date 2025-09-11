package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.campaign.CampaignCreateRequest;
import com.petek.inventoryService.dto.campaign.CampaignFilterRequest;
import com.petek.inventoryService.dto.campaign.CampaignProductFilterRequest;
import com.petek.inventoryService.dto.campaign.CampaignProductItemResponse;
import com.petek.inventoryService.dto.campaign.CampaignResponse;
import com.petek.inventoryService.dto.campaign.CampaignUpdateRequest;
import com.petek.inventoryService.entity.Campaign;
import com.petek.inventoryService.entity.Campaign.CampaignType;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.mapper.CampaignMapper;
import com.petek.inventoryService.repository.CampaignRepository;
import com.petek.inventoryService.repository.ProductRepository;
import com.petek.inventoryService.spec.CampaignSpecifications;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CampaignService {
    
    private final CampaignRepository repository;
    private final CampaignMapper mapper;

    private final ProductRepository productRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "campaignId", "campaignName", "campaignType", "startDate", "endDate", "updatedAt"
    );

    /**
     * Get all products.
     */
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> getAllCampaigns(CampaignFilterRequest request) {
        // Validate start date range
        if (request.getStartGte() != null && request.getStartLte() != null &&
            request.getStartGte().isAfter(request.getStartLte())) {
            throw new IllegalArgumentException("start_gte cannot be after start_lte");
        }

        // Validate end date range
        if (request.getEndGte() != null && request.getEndLte() != null &&
            request.getEndGte().isAfter(request.getEndLte())) {
            throw new IllegalArgumentException("end_gte cannot be after end_lte");
        }

        // Validate active_on falls within start and end dates if provided
        if (request.getActiveOn() != null) {
            if (request.getStartGte() != null && request.getActiveOn().isBefore(request.getStartGte())) {
                throw new IllegalArgumentException("active_on cannot be before start_gte");
            }
            if (request.getEndLte() != null && request.getActiveOn().isAfter(request.getEndLte())) {
                throw new IllegalArgumentException("active_on cannot be after end_lte");
            }
        }

        // Validate updatedAfter is not in the future
        if (request.getUpdatedAfter() != null && request.getUpdatedAfter().isAfter(Instant.now())) {
            throw new IllegalArgumentException("updated_after cannot be in the future");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<Campaign> spec = CampaignSpecifications.withFilters(request);

        Page<Campaign> campaignPage = repository.findAll(spec, pageable);

        List<CampaignResponse> campaignResponses = campaignPage.getContent()
            .stream()
            .map(mapper::toCampaignResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            campaignPage.getNumber(),
            campaignPage.getSize(),
            campaignPage.getTotalElements(),
            campaignPage.getTotalPages()
        );

        return new PageResponse<CampaignResponse>(campaignResponses, pageInfo);
    }

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
     * Get all campaign product.
     */
    public PageResponse<CampaignProductItemResponse> getAllCampaignProducts(Long campaignId, CampaignProductFilterRequest request) {
        repository.findById(campaignId)
            .orElseThrow(() -> new EntityNotFoundException("Campaign not found with id: " + campaignId));
        
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Page<Product> productPage = repository.findProductsByCampaignIdNative(campaignId, pageable);

        List<CampaignProductItemResponse> campaignProductItemResponses = productPage.getContent()
            .stream()
            .map(mapper::toCampaignProductItemResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            productPage.getNumber(),
            productPage.getSize(),
            productPage.getTotalElements(),
            productPage.getTotalPages()
        );

        return new PageResponse<CampaignProductItemResponse>(campaignProductItemResponses, pageInfo);
    }

    /**
     * Create campaign product.
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
     * Delete campaign product.
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
