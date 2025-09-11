package com.petek.inventoryService.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petek.inventoryService.entity.Campaign;
import com.petek.inventoryService.entity.Product;

public interface CampaignRepository extends JpaRepository<Campaign, Long>, JpaSpecificationExecutor<Campaign> {

    @Query(value = "SELECT p.* FROM products p " +
                   "INNER JOIN campaign_products cp ON p.product_id = cp.product_id " +
                   "WHERE cp.campaign_id = :campaignId " +
                   "ORDER BY p.product_id", 
           countQuery = "SELECT COUNT(*) FROM products p " +
                       "INNER JOIN campaign_products cp ON p.product_id = cp.product_id " +
                       "WHERE cp.campaign_id = :campaignId",
           nativeQuery = true)
    Page<Product> findProductsByCampaignIdNative(@Param("campaignId") Long campaignId, Pageable pageable);

}
