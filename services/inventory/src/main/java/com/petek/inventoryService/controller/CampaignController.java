package com.petek.inventoryService.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petek.inventoryService.dto.campaign.CampaignCreateRequest;
import com.petek.inventoryService.dto.campaign.CampaignResponse;
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

}
