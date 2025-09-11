package com.petek.inventoryService.dto.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignProductItemResponse {
    private Long productId;
    private String productName;
    private String category;
}
