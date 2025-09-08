package com.petek.inventoryService.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.stock.StockMovementCreateRequest;
import com.petek.inventoryService.dto.stock.StockMovementResponse;
import com.petek.inventoryService.entity.StockMovement;
import com.petek.inventoryService.entity.StockMovement.MovementKind;
import com.petek.inventoryService.entity.StockMovement.MovementSource;
import com.petek.inventoryService.mapper.StockMovementMapper;
import com.petek.inventoryService.repository.StockMovementRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository repository;
    private final StockMovementMapper mapper;

    private final ProductService productService;
    private final CurrentStockService currentStockService;
    
    /**
     * Create a Stock Movement.
     */
    public StockMovementResponse createStockMovement(StockMovementCreateRequest request) {
        // Control for product
        if (productService.getProductById(request.getProductId()) == null) {
            throw new EntityNotFoundException("Product not found with id: " + request.getProductId());
        }

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
        
        StockMovement stockMovement = mapper.toStockMovement(request);
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
