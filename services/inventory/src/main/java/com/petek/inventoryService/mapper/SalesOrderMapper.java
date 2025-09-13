package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.salesOrder.SalesOrderCreateRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemCreateRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemResponse;
import com.petek.inventoryService.dto.salesOrder.SalesOrderResponse;
import com.petek.inventoryService.entity.SalesOrder;
import com.petek.inventoryService.entity.SalesOrderItem;

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
            .deliveredAt(salesOrder.getDeliveredAt())
            .status(salesOrder.getStatus())
            .customerSpecialOfferId(salesOrder.getCustomerSpecialOfferId())
            .customerDiscountPctApplied(salesOrder.getCustomerDiscountPctApplied())
            .createdAt(salesOrder.getCreatedAt())
            .updatedAt(salesOrder.getUpdatedAt())
            .build();
    }

    /**
     * Map SalesOrderItemCreateRequest to SalesOrderItem entity.
     */
    public SalesOrderItem toSalesOrderItem(SalesOrderItemCreateRequest request) {
        return SalesOrderItem.builder()
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .build();
    }

    /**
     * Map SalesOrderItem entity to SalesOrderItemResponse.
     */
    public SalesOrderItemResponse toSalesOrderItemResponse(SalesOrderItem salesOrderItem) {
        return SalesOrderItemResponse.builder()
            .salesOrderItemId(salesOrderItem.getSalesOrderItemId())
            .salesOrderId(salesOrderItem.getSalesOrderId())
            .productId(salesOrderItem.getProductId())
            .quantity(salesOrderItem.getQuantity())
            .unitPrice(salesOrderItem.getUnitPrice())
            .discountPercentage(salesOrderItem.getDiscountPercentage())
            .campaignId(salesOrderItem.getCampaignId())
            .discountAmount(salesOrderItem.getDiscountAmount())
            .lineTotal(salesOrderItem.getLineTotal())
            .createdAt(salesOrderItem.getCreatedAt())
            .build();
    }

}
