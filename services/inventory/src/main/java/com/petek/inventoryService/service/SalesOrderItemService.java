package com.petek.inventoryService.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petek.inventoryService.dto.salesOrder.SalesOrderItemCreateRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemResponse;
import com.petek.inventoryService.entity.Campaign;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.SalesOrderItem;
import com.petek.inventoryService.mapper.SalesOrderMapper;
import com.petek.inventoryService.repository.CampaignRepository;
import com.petek.inventoryService.repository.ProductRepository;
import com.petek.inventoryService.repository.SalesOrderItemRepository;
import com.petek.inventoryService.repository.SalesOrderRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesOrderItemService {
    
    private final SalesOrderItemRepository repository;
    private final SalesOrderMapper mapper;

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;

    /**
     * Create a new sales order item.
     */
    public SalesOrderItemResponse createSalesOrderItem(Long salesOrderId, SalesOrderItemCreateRequest request) {
        // Validate SalesOrder
        salesOrderRepository.findById(salesOrderId)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order not found with id: " + salesOrderId));

        // Validate Product
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + request.getProductId()));

        SalesOrderItem salesOrderItem = mapper.toSalesOrderItem(request);
        salesOrderItem.setSalesOrderId(salesOrderId);
        salesOrderItem.setProductId(request.getProductId());
        salesOrderItem.setUnitPrice(product.getCurrentPrice());
        
        Campaign campaign = campaignRepository.findActiveCampaignsByProductAndDate(request.getProductId(), LocalDate.now());
        System.out.println(campaign);

        salesOrderItem.setDiscountPercentage(BigDecimal.ZERO);
        if(campaign != null) {
            salesOrderItem.setCampaignId(campaign.getCampaignId());
            if(campaign.getDiscountPercentage() != null) {
                salesOrderItem.setDiscountPercentage(campaign.getDiscountPercentage());
            } else {
                salesOrderItem.setDiscountPercentage(BigDecimal.valueOf(((campaign.getBuyQty()/(campaign.getBuyQty() + campaign.getGetQty())) * 100)));
            }
            salesOrderItem.setLineTotal(salesOrderItem.getQuantity().multiply(salesOrderItem.getUnitPrice()));
            salesOrderItem.setDiscountAmount(salesOrderItem.getLineTotal().multiply(salesOrderItem.getDiscountPercentage().divide(BigDecimal.valueOf(100))));
        }

        salesOrderItem.setCreatedAt(Instant.now());

        return mapper.toSalesOrderItemResponse(repository.save(salesOrderItem));
    }

}
