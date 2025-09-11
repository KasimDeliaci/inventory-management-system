package com.petek.inventoryService.controller;

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

}
