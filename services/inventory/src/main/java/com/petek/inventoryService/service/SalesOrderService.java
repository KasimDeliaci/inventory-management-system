package com.petek.inventoryService.service;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.saleOrder.SalesOrderCreateRequest;
import com.petek.inventoryService.dto.saleOrder.SalesOrderResponse;
import com.petek.inventoryService.entity.CustomerSpecialOffer;
import com.petek.inventoryService.entity.SalesOrder;
import com.petek.inventoryService.entity.SalesOrder.SalesOrderStatus;
import com.petek.inventoryService.mapper.SalesOrderMapper;
import com.petek.inventoryService.repository.CustomerRepository;
import com.petek.inventoryService.repository.CustomerSpecialOfferRepository;
import com.petek.inventoryService.repository.SalesOrderRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesOrderService {
    
    private final SalesOrderRepository repository;
    private final SalesOrderMapper mapper;

    private final CustomerRepository customerRepository;
    private final CustomerSpecialOfferRepository customerSpecialOfferRepository;

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

}
