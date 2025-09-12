package com.petek.inventoryService.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petek.inventoryService.entity.CustomerSpecialOffer;

public interface CustomerSpecialOfferRepository extends JpaRepository<CustomerSpecialOffer, Long>, JpaSpecificationExecutor<CustomerSpecialOffer> {
    
    @Query("SELECT cso FROM CustomerSpecialOffer cso " +
           "WHERE cso.customerId = :customerId " +
           "AND :checkDate BETWEEN cso.startDate AND cso.endDate")
    CustomerSpecialOffer findActiveSpecialOffers(@Param("customerId") Long customerId, 
                                                @Param("checkDate") LocalDate checkDate);

}
