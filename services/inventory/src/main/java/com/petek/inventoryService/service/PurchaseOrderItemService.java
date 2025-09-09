package com.petek.inventoryService.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemCreateRequest;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemFilterRequest;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemResponse;
import com.petek.inventoryService.dto.purchaseOrderItem.PurchaseOrderItemUpdateRequest;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.PurchaseOrder;
import com.petek.inventoryService.entity.PurchaseOrderItem;
import com.petek.inventoryService.mapper.PurchaseOrderItemMapper;
import com.petek.inventoryService.repository.ProductRepository;
import com.petek.inventoryService.repository.PurchaseOrderItemRepository;
import com.petek.inventoryService.repository.PurchaseOrderRepository;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderItemService {
    
    private final PurchaseOrderItemRepository repository;
    private final PurchaseOrderItemMapper mapper;

    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "purchaseOrderItemId", "productId", "quantityOrdered", "quantityReceived", "unitPrice", "lineTotal", "lineTotalReceived", "createdAt"
    );  
    
    /**
     * Get all products.
     */
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderItemResponse> getAllPurchaseOrderItem(Long purchaseOrderId, PurchaseOrderItemFilterRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));

        Page<PurchaseOrderItem> purchaseOrderItemPage = repository.findByPurchaseOrderPurchaseOrderId(purchaseOrderId, pageable);

        List<PurchaseOrderItemResponse> purchaseOrderItemResponses = purchaseOrderItemPage.getContent()
            .stream()
            .map(mapper::toPurchaseOrderItemItemResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            purchaseOrderItemPage.getNumber(),
            purchaseOrderItemPage.getSize(),
            purchaseOrderItemPage.getTotalElements(),
            purchaseOrderItemPage.getTotalPages()
        );

        return new PageResponse<PurchaseOrderItemResponse>(purchaseOrderItemResponses, pageInfo);
    }

    /**
     * Create a new purchase order item.
     */
    public PurchaseOrderItemResponse createPurchaseOrderItem(Long purchaseOrderItemId, PurchaseOrderItemCreateRequest request) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderItemId)
            .orElseThrow(() -> new EntityNotFoundException("Purches Order Item not found with id: " + purchaseOrderItemId));
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + request.getProductId()));
        
        PurchaseOrderItem purchaseOrderItem = mapper.toPurchaseOrderItem(request, product);
        purchaseOrderItem.setPurchaseOrder(purchaseOrder);
        purchaseOrderItem.setQuantityReceived(BigDecimal.ZERO);
        purchaseOrderItem.setLineTotalReceived(BigDecimal.ZERO);
        purchaseOrderItem.setCreatedAt(Instant.now());
        
        return mapper.toPurchaseOrderItemItemResponse(repository.save(purchaseOrderItem));
    }

    /**
     * Get purchase order item by id.
     */
    public PurchaseOrderItemResponse getPurchaseOrderItemById(Long purchaseOrderItemId) {
        return repository.findById(purchaseOrderItemId)
            .map(mapper::toPurchaseOrderItemItemResponse)
            .orElseThrow(() -> new EntityNotFoundException("Purches Order Item not found with id: " + purchaseOrderItemId));
    }

    /**
     * Update purchase order item.
     */
    public PurchaseOrderItemResponse updatePurchaseOrderItem(Long purchaseOrderItemId, PurchaseOrderItemUpdateRequest request) {
        PurchaseOrderItem existingPurchaseOrderItem = repository.findById(purchaseOrderItemId)
            .orElseThrow(() -> new EntityNotFoundException("Purchase Item Order not found with id: " + purchaseOrderItemId));
        
        Optional.ofNullable(request.getQuantityReceived())
            .ifPresent(existingPurchaseOrderItem::setQuantityReceived);
            
        Optional.ofNullable(request.getUnitPrice())
            .ifPresent(existingPurchaseOrderItem::setUnitPrice);

        return mapper.toPurchaseOrderItemItemResponse(repository.save(existingPurchaseOrderItem));
    }

    /**
     * Delete purchase order item.
     */
    public void deletePurchaseOrderItem(Long purchaseOrderItemId) {
        PurchaseOrderItem purchaseOrderItem = repository.findById(purchaseOrderItemId)
            .orElseThrow(() -> new EntityNotFoundException("Purchase Order Item not found with id: " + purchaseOrderItemId));
        repository.delete(purchaseOrderItem);
    }

}
