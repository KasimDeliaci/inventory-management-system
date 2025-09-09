package com.petek.inventoryService.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.PageResponse;
import com.petek.inventoryService.dto.PageResponse.PageInfo;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderCreateRequest;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderFilterRequest;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderResponse;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderUpdateRequest;
import com.petek.inventoryService.entity.PurchaseOrder;
import com.petek.inventoryService.entity.Supplier;
import com.petek.inventoryService.entity.PurchaseOrder.PurchaseOrderStatus;
import com.petek.inventoryService.mapper.PurchaseOrderMapper;
import com.petek.inventoryService.repository.PurchaseOrderRepository;
import com.petek.inventoryService.repository.SupplierRepository;
import com.petek.inventoryService.spec.PurchaseOrderSpecifications;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {
    
    private final PurchaseOrderRepository repository;
    private final PurchaseOrderMapper mapper;

    private final SupplierRepository supplierRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "purchaseOrderId", "supplierId", "orderDate", "expectedDelivery", "actualDelivery", "status", "updatedAt"
    );

    /**
     * Get all purchase order.
     */
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> getAllPurchaseOrder(PurchaseOrderFilterRequest request) {
        // Validate order date range
        if (request.getOrderDateGte() != null && request.getOrderDateLte() != null &&
            request.getOrderDateGte().isAfter(request.getOrderDateLte())) {
            throw new IllegalArgumentException("order_date_gte cannot be after order_date_lte");
        }

        // Validate expected delivery date range
        if (request.getExpectedDeliveryGte() != null && request.getExpectedDeliveryLte() != null &&
            request.getExpectedDeliveryGte().isAfter(request.getExpectedDeliveryLte())) {
            throw new IllegalArgumentException("expected_delivery_gte cannot be after expected_delivery_lte");
        }

        // Validate receivedSince vs updatedAfter (optional, if you want consistency checks)
        if (request.getReceivedSince() != null && request.getUpdatedAfter() != null &&
            request.getReceivedSince().isAfter(request.getUpdatedAfter())) {
            throw new IllegalArgumentException("received_since cannot be after updated_after");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<PurchaseOrder> spec = PurchaseOrderSpecifications.withFilters(request);

        Page<PurchaseOrder> purchaseOrderPage = repository.findAll(spec, pageable);

        List<PurchaseOrderResponse> purchaseOrderResponses = purchaseOrderPage.getContent()
            .stream()
            .map(mapper::toPurchaseOrderResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            purchaseOrderPage.getNumber(),
            purchaseOrderPage.getSize(),
            purchaseOrderPage.getTotalElements(),
            purchaseOrderPage.getTotalPages()
        );

        return new PageResponse<PurchaseOrderResponse>(purchaseOrderResponses, pageInfo);
    }

    /**
     * Create a new purchase order.
     */
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + request.getSupplierId()));

        PurchaseOrder purchaseOrder = mapper.toPurchaseOrder(request, supplier);
        purchaseOrder.setStatus(PurchaseOrderStatus.PLACED);
        purchaseOrder.setCreatedAt(Instant.now());
        purchaseOrder.setUpdatedAt(Instant.now());

        return mapper.toPurchaseOrderResponse(repository.save(purchaseOrder));
    }

    /**
     * Get a purchase order by id.
     */
    public PurchaseOrderResponse getPurchaseOrderById(Long purchaseOrderId) {
        return repository.findById(purchaseOrderId)
            .map(mapper::toPurchaseOrderResponse)
            .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found with id: " + purchaseOrderId));
    }

    /**
     * Update purchase order.
     */
    public PurchaseOrderResponse updatePurchaseOrder(Long purchaseOrderId, PurchaseOrderUpdateRequest request) {
        PurchaseOrder existingPurchaseOrder = repository.findById(purchaseOrderId)
            .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found with id: " + purchaseOrderId));
        
        Optional.ofNullable(request.getActualDelivery())
            .ifPresent(existingPurchaseOrder::setActualDelivery);
            
        Optional.ofNullable(request.getStatus())
            .ifPresent(existingPurchaseOrder::setStatus);

        existingPurchaseOrder.setUpdatedAt(Instant.now());

        return mapper.toPurchaseOrderResponse(repository.save(existingPurchaseOrder));
    }

    /**
     * Delete purchase order.
     */
    public void deletePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder existingPurchaseOrder = repository.findById(purchaseOrderId) 
            .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found with id: " + purchaseOrderId));
        repository.delete(existingPurchaseOrder);
    }

}
