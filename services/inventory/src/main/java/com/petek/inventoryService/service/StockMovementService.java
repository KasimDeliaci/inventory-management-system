package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.stock.StockMovementCreateRequest;
import com.petek.inventoryService.dto.stock.StockMovementFilterRequest;
import com.petek.inventoryService.dto.stock.StockMovementResponse;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.StockMovement;
import com.petek.inventoryService.entity.StockMovement.MovementKind;
import com.petek.inventoryService.entity.StockMovement.MovementSource;
import com.petek.inventoryService.mapper.StockMovementMapper;
import com.petek.inventoryService.repository.ProductRepository;
import com.petek.inventoryService.repository.StockMovementRepository;
import com.petek.inventoryService.spec.StockMovementSpecifications;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StockMovementService {

    private final StockMovementRepository repository;
    private final StockMovementMapper mapper;

    private final ProductRepository productRepository;
    private final CurrentStockService currentStockService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "movementId", "product", "movementKind", "movementSource", "quantity", "movementDate"
    );

    /**
     * Get All Stock Movements.
     */
    public PageResponse<StockMovementResponse> getAllStockMovements(StockMovementFilterRequest request) {
        // Validate movement date range
        if (request.getMovementDateGte() != null && request.getMovementDateLte() != null && 
            request.getMovementDateGte().isAfter(request.getMovementDateLte())) {
            throw new IllegalArgumentException("movement_date_gte cannot be after movement_date_lte");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<StockMovement> spec = StockMovementSpecifications.withFilters(request);

        Page<StockMovement> stockMovementPage = repository.findAll(spec, pageable);

        List<StockMovementResponse> stockMovementResponses = stockMovementPage.getContent()
            .stream()
            .map(mapper::toStockMovementResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            stockMovementPage.getNumber(),
            stockMovementPage.getSize(),
            stockMovementPage.getTotalElements(),
            stockMovementPage.getTotalPages()
        );

        return new PageResponse<StockMovementResponse>(stockMovementResponses, pageInfo);
    }

    /**
     * Create a Stock Movement.
     */
    public StockMovementResponse createStockMovement(StockMovementCreateRequest request) {
        // Control for movement kind
        if (request.getMovementKind() != MovementKind.ADJUSTMENT_IN && request.getMovementKind() != MovementKind.ADJUSTMENT_OUT) {
            throw new IllegalArgumentException("Invalid movement kind. Must be ADJUSTMENT_IN or ADJUSTMENT_OUT.");
        }
        
        // Control for available quantity
        if (request.getMovementKind() == MovementKind.ADJUSTMENT_OUT
        && currentStockService.getAvailableQuantityById(request.getProductId()).compareTo(request.getQuantity()) < 0
        ) {
            throw new IllegalArgumentException("Not enough stock in inventory");
        }
        
        // Getting product
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + request.getProductId()));
            
        StockMovement stockMovement = mapper.toStockMovement(request, product);
        stockMovement.setMovementSource(MovementSource.ADJUSTMENT);
        stockMovement.setMovementDate(Instant.now());
        stockMovement.setCreatedAt(Instant.now());
        
        StockMovement savedStockMovement = repository.save(stockMovement);

        if (request.getMovementKind() == MovementKind.ADJUSTMENT_IN) {
            currentStockService.updateStockIn(request.getProductId(), savedStockMovement.getMovementId(), request.getQuantity());
        } else {
            currentStockService.updateStockOut(request.getProductId(), stockMovement.getMovementId(), request.getQuantity(), false);
        }

        return mapper.toStockMovementResponse(savedStockMovement);
    }

}
