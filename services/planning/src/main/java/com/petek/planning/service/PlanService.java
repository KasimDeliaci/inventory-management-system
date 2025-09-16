package com.petek.planning.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petek.planning.dto.PlanRequest;
import com.petek.planning.dto.PlanResponse;
import com.petek.planning.entity.PlanRecommendation;

import com.petek.planning.repository.PlanRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanService {
    
    private final PlanRepository repository;
    private final ObjectMapper mapper;

    private final OllamaService ollamaService;
    private final WebClientService webClientService;

    /**
     * Get ai output with other inputs
     */
    public PlanResponse getAiOutput(PlanRequest request) {
        
        String product = webClientService.getProduct(request.getProductId());
        String stock = webClientService.getCurrentStock(request.getProductId());
        String suppliers = webClientService.getSuppliers(request.getProductId());
        String forecasts = webClientService.getForecasts(request.getProductId(), Integer.parseInt(request.getHorizonDays()), request.getAsOfDate());

        String returnValue = ollamaService.callOllama(product, stock, suppliers, forecasts);

        Long forecastId = Long.valueOf(0);
        try {
            JsonNode actualObj = mapper.readTree(forecasts);
            forecastId = actualObj.path("forecastId").asLong();
        } catch (Exception e) {
            System.out.println("Error");
        }

        repository.save(PlanRecommendation.builder()
            .forecastId(forecastId)
            .productId(request.getProductId())
            .asOfDate(request.getAsOfDate())
            .horizonDays(Integer.parseInt(request.getHorizonDays()))
            .responseJson(returnValue)
            .model(request.getModel())
            .createdAt(Instant.now())
            .build());

        return PlanResponse.builder().message(returnValue).build();
    }

}
