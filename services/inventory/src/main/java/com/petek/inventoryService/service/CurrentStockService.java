package com.petek.inventoryService.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.stock.CurrentStockFilterRequest;
import com.petek.inventoryService.dto.stock.CurrentStockResponse;
import com.petek.inventoryService.entity.CurrentStock;
import com.petek.inventoryService.entity.StockMovement;
import com.petek.inventoryService.mapper.CurrentStockMapper;
import com.petek.inventoryService.repository.CurrentStockRepository;
import com.petek.inventoryService.repository.StockMovementRepository;
import com.petek.inventoryService.spec.CurrentStockSpecifications;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentStockService {
    
    private final CurrentStockRepository repository;
    private final CurrentStockMapper mapper;

    private final StockMovementRepository stockMovementRepository;
    private final ProductService productService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "productId", "quantityOnHand", "quantityReserved", "quantityAvailable", "lastUpdated"
    );

    /**
     * Get all Current Stock.
     */
    public PageResponse<CurrentStockResponse> getAllCurrentStocks(CurrentStockFilterRequest request) {
        // Validations

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<CurrentStock> spec = CurrentStockSpecifications.withFilters(request);

        Page<CurrentStock> currentStockPage = repository.findAll(spec, pageable);

        List<CurrentStockResponse> currentStockResponses = currentStockPage.getContent()
            .stream()
            .map(mapper::toCurrentStockResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            currentStockPage.getNumber(),
            currentStockPage.getSize(),
            currentStockPage.getTotalElements(),
            currentStockPage.getTotalPages()
        );

        return new PageResponse<CurrentStockResponse>(currentStockResponses, pageInfo);
    }

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
