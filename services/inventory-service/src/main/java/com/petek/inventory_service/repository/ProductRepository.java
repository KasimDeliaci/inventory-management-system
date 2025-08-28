package com.petek.inventory_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petek.inventory_service.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{
    
}
