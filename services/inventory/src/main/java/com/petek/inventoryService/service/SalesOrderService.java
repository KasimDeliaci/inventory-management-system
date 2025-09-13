package com.petek.inventoryService.service;

import java.time.Instant;
import java.time.LocalDate;
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
import com.petek.inventoryService.dto.salesOrder.SalesOrderCreateRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderFilterRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderResponse;
import com.petek.inventoryService.dto.salesOrder.SalesOrderUpdateRequest;
import com.petek.inventoryService.entity.CustomerSpecialOffer;
import com.petek.inventoryService.entity.SalesOrder;
import com.petek.inventoryService.entity.SalesOrder.SalesOrderStatus;
import com.petek.inventoryService.mapper.SalesOrderMapper;
import com.petek.inventoryService.repository.CustomerRepository;
import com.petek.inventoryService.repository.CustomerSpecialOfferRepository;
import com.petek.inventoryService.repository.SalesOrderRepository;
import com.petek.inventoryService.spec.SalesOrderSpecifications;
import com.petek.inventoryService.utils.SortUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesOrderService {
    
    private final SalesOrderRepository repository;
    private final SalesOrderMapper mapper;

    private final CustomerRepository customerRepository;
    private final CustomerSpecialOfferRepository customerSpecialOfferRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "salesOrderId"
    );  
    
    /**
     * Get all products.
     */
    @Transactional(readOnly = true)
    public PageResponse<SalesOrderResponse> getAllSalesOrders(SalesOrderFilterRequest request) {
        // Validate order date range
        if (request.getOrderDateGte() != null && request.getOrderDateLte() != null && 
            request.getOrderDateGte().isAfter(request.getOrderDateLte())) {
            throw new IllegalArgumentException("order_date_gte cannot be greater than order_date_lte");
        }

        // Validate delivery date range
        if (request.getDeliveryDateGte() != null && request.getDeliveryDateLte() != null && 
            request.getDeliveryDateGte().isAfter(request.getDeliveryDateLte())) {
            throw new IllegalArgumentException("delivery_date_gte cannot be greater than delivery_date_lte");
        }

        // Validate delivered since and updated after timestamps
        if (request.getDeliveredSince() != null && request.getUpdatedAfter() != null && 
            request.getDeliveredSince().isAfter(request.getUpdatedAfter())) {
            throw new IllegalArgumentException("delivered_since cannot be greater than updated_after");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));
        Specification<SalesOrder> spec = SalesOrderSpecifications.withFilters(request);

        Page<SalesOrder> salesOrderPage = repository.findAll(spec, pageable);

        List<SalesOrderResponse> salesOrderResponses = salesOrderPage.getContent()
            .stream()
            .map(mapper::toSalesOrderResponse)
            .toList();

        PageInfo pageInfo = new PageInfo(
            salesOrderPage.getNumber(),
            salesOrderPage.getSize(),
            salesOrderPage.getTotalElements(),
            salesOrderPage.getTotalPages()
        );

        return new PageResponse<SalesOrderResponse>(salesOrderResponses, pageInfo);
    }

    /**
     * Create a new sales order.
     */
    public SalesOrderResponse createSalesOrder(SalesOrderCreateRequest request) {
        customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));

        SalesOrder salesOrder = mapper.toSalesOrder(request);
        salesOrder.setOrderDate(LocalDate.now());
        salesOrder.setStatus(SalesOrderStatus.PENDING);
        
        CustomerSpecialOffer customerSpecialOffer = customerSpecialOfferRepository.findActiveSpecialOffers(request.getCustomerId(), salesOrder.getOrderDate());

        if (customerSpecialOffer != null) {
            salesOrder.setCustomerSpecialOfferId(customerSpecialOffer.getSpecialOfferId());
            salesOrder.setCustomerDiscountPctApplied(customerSpecialOffer.getPercentOff());
        }

        salesOrder.setCreatedAt(Instant.now());
        salesOrder.setUpdatedAt(Instant.now());
        return mapper.toSalesOrderResponse(repository.save(salesOrder));
    }

    /**
     * Get a sales order by id.
     */
    public SalesOrderResponse getSalesOrderById(Long salesOrderId) {
        return repository.findById(salesOrderId)
            .map(mapper::toSalesOrderResponse)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order not found with id: " + salesOrderId));
    }

    /**
     * Update a sales order.
     */
    public SalesOrderResponse updateSalesOrder(Long salesOrderId, SalesOrderUpdateRequest request) {
        SalesOrder existingSalesOrder = repository.findById(salesOrderId)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order not found with id: " + salesOrderId));

        Optional.ofNullable(request.getDeliveryDate())
            .ifPresent(existingSalesOrder::setDeliveryDate);
        
        if (request.getStatus() == SalesOrderStatus.DELIVERED && existingSalesOrder.getStatus() != SalesOrderStatus.DELIVERED) {
            existingSalesOrder.setDeliveredAt(Instant.now());
        }

        Optional.ofNullable(request.getStatus())
            .ifPresent(existingSalesOrder::setStatus);

        existingSalesOrder.setUpdatedAt(Instant.now());

        return mapper.toSalesOrderResponse(repository.save(existingSalesOrder));
    }

    /**
     * Delete a sales order.
     */
    public void deleteSalesOrder(Long salesOrderId) {
        SalesOrder existingSalesOrder = repository.findById(salesOrderId)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order not found with id: " + salesOrderId));
        repository.delete(existingSalesOrder);
    }

}
