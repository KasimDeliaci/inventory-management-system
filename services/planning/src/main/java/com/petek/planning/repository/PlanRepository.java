package com.petek.planning.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.planning.entity.PlanRecommendation;

public interface PlanRepository extends JpaRepository<PlanRecommendation, Long> {
    
}
