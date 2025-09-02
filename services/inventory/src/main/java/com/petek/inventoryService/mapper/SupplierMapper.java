package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.SupplierCreateRequest;
import com.petek.inventoryService.dto.SupplierResponse;
import com.petek.inventoryService.entity.Supplier;

@Service
public class SupplierMapper {

    public Supplier toSupplier(SupplierCreateRequest request) {
        return Supplier.builder()
            .supplierName(request.supplierName())
            .email(request.email())
            .phone(request.phone())
            .city(request.city())
            .build();
    }

    public SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
            supplier.getSupplierId(),
            supplier.getSupplierName(),
            supplier.getEmail(),
            supplier.getPhone(),
            supplier.getCity(),
            supplier.getCreatedAt()
        );
    }
}
