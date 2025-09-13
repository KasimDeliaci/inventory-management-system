package com.petek.inventoryService.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemCreateRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemFilterRequest;
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemResponse;
import com.petek.inventoryService.dto.salesOrder.SalesOrderItemUpdateRequest;
import com.petek.inventoryService.entity.Campaign;
import com.petek.inventoryService.entity.Product;
import com.petek.inventoryService.entity.SalesOrderItem;
import com.petek.inventoryService.mapper.SalesOrderMapper;
import com.petek.inventoryService.repository.CampaignRepository;
import com.petek.inventoryService.repository.ProductRepository;
import com.petek.inventoryService.repository.SalesOrderItemRepository;
import com.petek.inventoryService.repository.SalesOrderRepository;
import com.petek.inventoryService.utils.SortUtils;

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

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "salesOrderItemId", "productId", "quantity", "unitPrice", "discountPercentage", "discountAmount", "lineTotal", "createdAt"
    );  
    
    /**
     * Get all products.
     */
    @Transactional(readOnly = true)
    public PageResponse<SalesOrderItemResponse> getAllSalesOrderItem(Long salesOrderId, SalesOrderItemFilterRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), SortUtils.createSort(request.getSort(), ALLOWED_SORT_FIELDS));

        Page<SalesOrderItem> salesOrderItemPage = repository.findBySalesOrderId(salesOrderId, pageable);

        List<SalesOrderItemResponse> salesOrderItemResponses = salesOrderItemPage.getContent()
            .stream()
            .map(mapper::toSalesOrderItemResponse)
            .toList();
        
        PageInfo pageInfo = new PageInfo(
            salesOrderItemPage.getNumber(),
            salesOrderItemPage.getSize(),
            salesOrderItemPage.getTotalElements(),
            salesOrderItemPage.getTotalPages()
        );

        return new PageResponse<SalesOrderItemResponse>(salesOrderItemResponses, pageInfo);
    }

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

    /**
     * Get a new sales order item by id.
     */
    public SalesOrderItemResponse getSalesOrderItemById(Long salesOrderId, Long salesOrderItemId) {
        SalesOrderItem salesOrderItem = repository.findById(salesOrderItemId)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order Item not found with id: " + salesOrderItemId));
        
        if(salesOrderItem.getSalesOrderId() != salesOrderId) {
            throw new IllegalArgumentException("This SalesOrder dont have this SalesOrderItem");
        }

        return mapper.toSalesOrderItemResponse(salesOrderItem);            
    }

    /**
     * Update a new sales order item.
     */
    public SalesOrderItemResponse updateSalesOrderItem(Long salesOrderId, Long salesOrderItemId, SalesOrderItemUpdateRequest request) {
        SalesOrderItem existingSalesOrderItem = repository.findById(salesOrderItemId)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order Item not found with id: " + salesOrderItemId));
        
        if(existingSalesOrderItem.getSalesOrderId() != salesOrderId) {
            throw new IllegalArgumentException("This SalesOrder dont have this SalesOrderItem");
        }

        Optional.ofNullable(request.getQuantity())
            .ifPresent(existingSalesOrderItem::setQuantity);

        return mapper.toSalesOrderItemResponse(repository.save(existingSalesOrderItem));            
    }

    /**
     * Delete a new sales order item.
     */
    public void deleteSalesOrderItem(Long salesOrderId, Long salesOrderItemId) {
        SalesOrderItem salesOrderItem = repository.findById(salesOrderItemId)
            .orElseThrow(() -> new EntityNotFoundException("Sales Order Item not found with id: " + salesOrderItemId));
        
        if(salesOrderItem.getSalesOrderId() != salesOrderId) {
            throw new IllegalArgumentException("This SalesOrder dont have this SalesOrderItem");
        }

        repository.delete(salesOrderItem);         
    }

}
