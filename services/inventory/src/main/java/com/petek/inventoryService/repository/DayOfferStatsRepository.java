package com.petek.inventoryService.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.petek.inventoryService.entity.DayOfferStats;

public interface DayOfferStatsRepository extends JpaRepository<DayOfferStats, LocalDate>, JpaSpecificationExecutor<DayOfferStats> {
    
}
