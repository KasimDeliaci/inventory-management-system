package com.petek.inventoryService.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderCreateRequest;
import com.petek.inventoryService.dto.purchaseOrder.PurchaseOrderResponse;
import com.petek.inventoryService.entity.PurchaseOrder;
import com.petek.inventoryService.entity.Supplier;
import com.petek.inventoryService.entity.PurchaseOrder.PurchaseOrderStatus;
import com.petek.inventoryService.mapper.PurchaseOrderMapper;
import com.petek.inventoryService.repository.PurchaseOrderRepository;
import com.petek.inventoryService.repository.SupplierRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {
    
    private final PurchaseOrderRepository repository;
    private final PurchaseOrderMapper mapper;

    private final SupplierRepository supplierRepository;

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

}
