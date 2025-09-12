package com.petek.inventoryService.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.reporting.ReportingRequest;
import com.petek.inventoryService.dto.reporting.DayOfferStatsResponse;
import com.petek.inventoryService.dto.reporting.ProductDayPromoResponse;
import com.petek.inventoryService.dto.reporting.ProductDaySalesResponse;
import com.petek.inventoryService.entity.DayOfferStats;
import com.petek.inventoryService.entity.ProductDayPromo;
import com.petek.inventoryService.entity.ProductDaySales;
import com.petek.inventoryService.mapper.ReportingMapper;
import com.petek.inventoryService.repository.DayOfferStatsRepository;
import com.petek.inventoryService.repository.ProductDayPromoRepository;
import com.petek.inventoryService.repository.ProductDaySalesRepository;
import com.petek.inventoryService.spec.ReportingSpecifications;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportingService {
    
    private final ProductDaySalesRepository productDaySalesRepository;
    private final ProductDayPromoRepository productDayPromoRepository;
    private final DayOfferStatsRepository dayOfferStatsRepository;
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
        Specification<ProductDaySales> spec = ReportingSpecifications.withFiltersSales(request);

        return productDaySalesRepository.findAll(spec, sort)
            .stream()
            .map(mapper::toPoProductDaySalesResponse)
            .toList();
    }

    /**
     * Get all product day promos.
     */
    public List<ProductDayPromoResponse> getAllProductDayPromos(ReportingRequest request) {
        // Validate date range
        if (request.getFrom() != null && request.getTo() != null && 
            request.getFrom().compareTo(request.getTo()) > 0) {
            throw new IllegalArgumentException("from cannot be greater than to");
        }
        Sort sort = Sort.by("date");
        Specification<ProductDayPromo> spec = ReportingSpecifications.withFiltersPromo(request);

        return productDayPromoRepository.findAll(spec, sort)
            .stream()
            .map(mapper::toProductDayPromoResponse)
            .toList();
    }

    /**
     * Get all day offer stats.
     */
    public List<DayOfferStatsResponse> getDayOfferStats(ReportingRequest request) {
        // Validate date range
        if (request.getFrom() != null && request.getTo() != null && 
            request.getFrom().compareTo(request.getTo()) > 0) {
            throw new IllegalArgumentException("from cannot be greater than to");
        }
        Sort sort = Sort.by("date");
        Specification<DayOfferStats> spec = ReportingSpecifications.withFiltersOfferStats(request);

        return dayOfferStatsRepository.findAll(spec, sort)
            .stream()
            .map(mapper::toDayOfferStatsResponse)
            .toList();
    }

}
