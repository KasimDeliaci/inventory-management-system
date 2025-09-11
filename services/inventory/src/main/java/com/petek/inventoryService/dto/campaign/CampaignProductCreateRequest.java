package com.petek.inventoryService.dto.campaign;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignProductCreateRequest {
    private List<Long> productIds;
}
