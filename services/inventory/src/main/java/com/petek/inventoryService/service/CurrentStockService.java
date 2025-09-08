package com.petek.inventoryService.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.entity.CurrentStock;
import com.petek.inventoryService.entity.StockMovement;
import com.petek.inventoryService.repository.CurrentStockRepository;
import com.petek.inventoryService.repository.StockMovementRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentStockService {
    
    private final CurrentStockRepository repository;

    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;

    /**
     * Get a Current Stock of product.
     */
    public BigDecimal getAvailableQuantityById(Long productId) {
        Optional<CurrentStock> currentStock = repository.findById(productId);

        if(currentStock.isPresent()) return currentStock.get().getQuantityAvailable();
        else return BigDecimal.ZERO;
    }

    /**
     * Update/Create a Current Stock In.
     */
    public void updateStockIn(Long productId, Long stockMovementId, BigDecimal quantity) {
        // Control for product
        if (productService.getProductById(productId) == null) {
            throw new EntityNotFoundException("Product not found with id: " + productId);
        }
        
        // Control for stock movement
        StockMovement stockMovement = stockMovementRepository.findById(stockMovementId)
            .orElseThrow(() -> new EntityNotFoundException("Stock Movement not found with id: " + stockMovementId));

        Optional<CurrentStock> currentStock = repository.findById(productId);

        if (currentStock.isPresent()) {
            CurrentStock stock = currentStock.get();

            stock.setQuantityOnHand(stock.getQuantityOnHand().add(quantity));
            stock.setLastMovement(stockMovement);
            stock.setLastUpdated(Instant.now());

            repository.save(stock);
        } else {
            CurrentStock newStock = CurrentStock.builder()
                .productId(productId)
                .quantityOnHand(quantity)
                .quantityReserved(BigDecimal.ZERO)
                .quantityAvailable(quantity)
                .lastMovement(stockMovement)
                .lastUpdated(Instant.now())
                .build();
            repository.save(newStock);
        }

    }

    /**
     * Update/Create a Current Stock Out.
     */
    public void updateStockOut(Long productId, Long stockMovementId, BigDecimal quantity, boolean isReserve) {
        // Control for product
        if (productService.getProductById(productId) == null) {
            throw new EntityNotFoundException("Product not found with id: " + productId);
        }

        // Control for available quantity
        if (getAvailableQuantityById(productId).compareTo(quantity) < 0) {
            throw new IllegalArgumentException("Not enough stock in inventory");
        }
        
        // Control for stock movement
        StockMovement stockMovement = stockMovementRepository.findById(stockMovementId)
            .orElseThrow(() -> new EntityNotFoundException("Stock Movement not found with id: " + stockMovementId));

        Optional<CurrentStock> currentStock = repository.findById(productId);

        if (currentStock.isPresent()) {
            CurrentStock stock = currentStock.get();

            if (isReserve) {
                stock.setQuantityReserved(stock.getQuantityReserved().add(quantity));
                stock.setQuantityAvailable(stock.getQuantityAvailable().subtract(quantity));
            } else {
                stock.setQuantityOnHand(stock.getQuantityOnHand().subtract(quantity));
                stock.setQuantityAvailable(stock.getQuantityAvailable().subtract(quantity));
            }

            stock.setLastMovement(stockMovement);
            stock.setLastUpdated(Instant.now());

            repository.save(stock);
        } else {
            throw new IllegalArgumentException("Not enough stock in inventory");
        }
    }

}
