package com.petek.inventoryService.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemCreateRequest;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemResponse;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.PurchaseOrder;
import com.petek.inventoryService.entity.PurchaseOrderItem;
import com.petek.inventoryService.mapper.PurchaseOrderItemMapper;
import com.petek.inventoryService.repository.ProductRepository;
import com.petek.inventoryService.repository.PurchaseOrderItemRepository;
import com.petek.inventoryService.repository.PurchaseOrderRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderItemService {
    
    private final PurchaseOrderItemRepository repository;
    private final PurchaseOrderItemMapper mapper;

    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    /**
     * Create a new purchase order item.
     */
    public PurchaseOrderItemResponse createPurchaseOrderItem(Long purchaseOrderId, PurchaseOrderItemCreateRequest request) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
            .orElseThrow(() -> new EntityNotFoundException("Purches Order not found with id: " + purchaseOrderId));
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + purchaseOrderId));
        
        PurchaseOrderItem purchaseOrderItem = mapper.toPurchaseOrderItem(request, product);
        purchaseOrderItem.setPurchaseOrder(purchaseOrder);
        purchaseOrderItem.setQuantityReceived(BigDecimal.ZERO);
        purchaseOrderItem.setLineTotalReceived(BigDecimal.ZERO);
        purchaseOrderItem.setLineTotal(request.getQuantityOrdered().multiply(request.getUnitPrice()));
        purchaseOrderItem.setCreatedAt(Instant.now());
        
        return mapper.toPurchaseOrderItemItemResponse(repository.save(purchaseOrderItem));
    }

}
