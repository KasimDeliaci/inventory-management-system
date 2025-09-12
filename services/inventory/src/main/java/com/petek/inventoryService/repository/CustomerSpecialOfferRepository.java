package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventoryService.entity.CustomerSpecialOffer;

public interface CustomerSpecialOfferRepository extends JpaRepository<CustomerSpecialOffer, Long> {
    
}
