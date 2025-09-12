package com.petek.inventoryService.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.reporting.ReportingRequest;
import com.petek.inventoryService.dto.reporting.ProductDaySalesResponse;
import com.petek.inventoryService.entity.ProductDaySales;
import com.petek.inventoryService.mapper.ReportingMapper;
import com.petek.inventoryService.repository.ProductDaySalesRepository;
import com.petek.inventoryService.spec.ReportingSpecifications;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportingService {
    
    private final ProductDaySalesRepository productDaySalesRepository;
    private final ReportingMapper mapper;

    /**
     * Get all product day sales.
     */
    public List<ProductDaySalesResponse> getAllProductDaySale(ReportingRequest request) {
        // Validate date range
        if (request.getFrom() != null && request.getTo() != null && 
            request.getFrom().compareTo(request.getTo()) > 0) {
            throw new IllegalArgumentException("from cannot be greater than to");
        }
        Sort sort = Sort.by("date");
        Specification<ProductDaySales> spec = ReportingSpecifications.withFilters(request);

        return productDaySalesRepository.findAll(spec, sort)
            .stream()
            .map(mapper::toPoProductDaySalesResponse)
            .toList();
    }

}
