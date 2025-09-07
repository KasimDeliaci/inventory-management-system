package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.supplier.SupplierCreateRequest;
import com.petek.inventoryService.dto.supplier.SupplierResponse;
import com.petek.inventoryService.entity.Supplier;

@Service
public class SupplierMapper {

    /**
     * Map SupplierCreateRequest to Supplier entity.
     */
    public Supplier toSupplier(SupplierCreateRequest request) {
        return Supplier.builder()
            .supplierName(request.getSupplierName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .city(request.getCity())
            .build();
    }

    /**
     * Map Supplier entity to SupplierResponse.
     */
    public SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
            .supplierId(supplier.getSupplierId())
            .supplierName(supplier.getSupplierName())
            .email(supplier.getEmail())
            .phone(supplier.getPhone())
            .city(supplier.getCity())
            .createdAt(supplier.getCreatedAt())
            .build();
    }

}
