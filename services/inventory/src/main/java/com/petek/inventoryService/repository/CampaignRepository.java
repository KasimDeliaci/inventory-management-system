package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventoryService.entity.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    
}
