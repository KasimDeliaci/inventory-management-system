package com.petek.inventoryService.mapper;

import org.springframework.stereotype.Service;

import com.petek.inventoryService.dto.reporting.ProductDayPromoResponse;
import com.petek.inventoryService.dto.reporting.ProductDaySalesResponse;
import com.petek.inventoryService.entity.ProductDayPromo;
import com.petek.inventoryService.entity.ProductDaySales;

@Service
public class ReportingMapper {
    
    /**
     * Map ProductDaySales to ProductDaySalesResponse.
     */
    public ProductDaySalesResponse toPoProductDaySalesResponse(ProductDaySales productDaySales) {
        return ProductDaySalesResponse.builder()
            .date(productDaySales.getDate())
            .productId(productDaySales.getProductId())
            .salesUnits(productDaySales.getSalesUnits())
            .offerActiveShare(productDaySales.getOfferActiveShare())
            .build();
    }

    /**
     * Map ProductDayPromo to ProductDayPromoResponse.
     */
    public ProductDayPromoResponse toProductDayPromoResponse(ProductDayPromo productDayPromo) {
        return ProductDayPromoResponse.builder()
            .date(productDayPromo.getDate())
            .productId(productDayPromo.getProductId())
            .promoPct(productDayPromo.getPromoPct())
            .build();
    }

}
