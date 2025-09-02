package com.petek.inventoryService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventoryService.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long>{
    
}