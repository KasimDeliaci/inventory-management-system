package com.petek.inventoryService.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.campaign.CampaignCreateRequest;
import com.petek.inventoryService.dto.campaign.CampaignProductCreateRequest;
import com.petek.inventoryService.dto.campaign.CampaignResponse;
import com.petek.inventoryService.dto.campaign.CampaignUpdateRequest;
import com.petek.inventoryService.service.CampaignService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    
    private final CampaignService service;

    /**
     * Create a new campaign.
     */
    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(
        @RequestBody @Valid CampaignCreateRequest request
    ) {
        return ResponseEntity.status(201).body(service.createCampaign(request));
    }

    /**
     * Get a campaign by id.
     */
    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> getCampaignById(
        @PathVariable Long campaignId
    ) {
        return ResponseEntity.ok(service.getCampaignById(campaignId));
    }

    /**
     * Update a campaign.
     */
    @PutMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> updateCampaign(
        @PathVariable Long campaignId,
        @RequestBody @Valid CampaignUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateCampaign(campaignId, request));
    }

    /**
     * Delete a campaign.
     */
    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> deleteCampaign(
        @PathVariable Long campaignId
    ) {
        service.deleteCampaign(campaignId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

    /**
     * Create campaign product.
     */
    @PostMapping("/{campaignId}/products")
    public ResponseEntity<Void> assignCampaignProduct(
        @PathVariable Long campaignId,
        @RequestBody @Valid CampaignProductCreateRequest request
    ) {
        service.assignCampaignProduct(campaignId, request.getProductIds());
        return ResponseEntity.noContent().header("X-Assign-Description", "Assigned").build();
    }

    /**
     * Create campaign product by id.
     */
    @PostMapping("/{campaignId}/products/{productId}")
    public ResponseEntity<Void> assignCampaignProductById(
        @PathVariable Long campaignId,
        @PathVariable Long productId
    ) {
        service.assignCampaignProduct(campaignId, List.of(productId));
        return ResponseEntity.noContent().header("X-Assign-Description", "Assigned").build();
    }

    /**
     * Delete a campaign product by id.
     */
    @DeleteMapping("/{campaignId}/products/{productId}")
    public ResponseEntity<Void> deleteCampaignProductById(
        @PathVariable Long campaignId,
        @PathVariable Long productId
    ) {
        service.deleteCampaignProduct(campaignId, productId);
        return ResponseEntity.noContent().header("X-Delete-Description", "Deleted").build();
    }

}
