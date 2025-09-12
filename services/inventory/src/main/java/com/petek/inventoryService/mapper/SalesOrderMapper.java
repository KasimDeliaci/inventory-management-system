package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.saleOrder.SalesOrderCreateRequest;
import com.petek.inventoryService.dto.saleOrder.SalesOrderResponse;
import com.petek.inventoryService.entity.SalesOrder;

@Service
public class SalesOrderMapper {
    
    /**
     * Map SalesOrderCreateRequest to SalesOrder entity.
     */
    public SalesOrder toSalesOrder(SalesOrderCreateRequest request) {
        return SalesOrder.builder()
            .customerId(request.getCustomerId())
            .deliveryDate(request.getDeliveryDate())
            .build();
    }

    /**
     * Map SalesOrder entity to SalesOrderResponse.
     */
    public SalesOrderResponse toSalesOrderResponse(SalesOrder salesOrder) {
        return SalesOrderResponse.builder()
            .salesOrderId(salesOrder.getSalesOrderId())
            .customerId(salesOrder.getCustomerId())
            .orderDate(salesOrder.getOrderDate())
            .deliveryDate(salesOrder.getDeliveryDate())
            .status(salesOrder.getStatus())
            .customerSpecialOfferId(salesOrder.getCustomerSpecialOfferId())
            .customerDiscountPctApplied(salesOrder.getCustomerDiscountPctApplied())
            .createdAt(salesOrder.getCreatedAt())
            .updatedAt(salesOrder.getUpdatedAt())
            .build();
    }

}
