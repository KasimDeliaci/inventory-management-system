package com.petek.planning.service;

import org.springframework.stereotype.Service;

import com.petek.planning.dto.PlanRequest;
import com.petek.planning.repository.PlanRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanService {
    
    private final PlanRepository repository;

    private final OllamaService ollamaService;
    private final WebClientService webClientService;

    /**
     * Get ai output with other inputs
     */
    public String getAiOutput(PlanRequest request) {
        String products = ""; // add web client service
        String forecasts = "{\\\"forecasts\\\":[{\\\"productId\\\":1003,\\\"daily\\\":[{\\\"date\\\":\\\"2025-07-01\\\",\\\"yhat\\\":1},{\\\"date\\\":\\\"2025-07-02\\\",\\\"yhat\\\":1},{\\\"date\\\":\\\"2025-07-03\\\",\\\"yhat\\\":1},{\\\"date\\\":\\\"2025-07-04\\\",\\\"yhat\\\":1},{\\\"date\\\":\\\"2025-07-05\\\",\\\"yhat\\\":1},{\\\"date\\\":\\\"2025-07-06\\\",\\\"yhat\\\":1},{\\\"date\\\":\\\"2025-07-07\\\",\\\"yhat\\\":1}],\\\"sum\\\":7,\\\"predictionInterval\\\":{\\\"lowerBound\\\":0,\\\"upperBound\\\":33},\\\"confidence\\\":{\\\"score\\\":70,\\\"level\\\":\\\"high\\\",\\\"factors\\\":{\\\"volatility\\\":56,\\\"trend\\\":64,\\\"seasonality\\\":0,\\\"data_quality\\\":100,\\\"days_available\\\":373},\\\"recommendation\\\":\\\"reliable_for_planning\\\"}}],\\\"modelVersion\\\":\\\"xgb_three-20250913132214\\\",\\\"modelType\\\":\\\"xgb_three\\\",\\\"generatedAt\\\":\\\"2025-09-14T14:33:22.724374\\\",\\\"forecastId\\\":36}"; // add web client service

        // System.out.println(webClientService.getProduct(request.getProductId()));
        // System.out.println();
        // System.out.println(webClientService.getSuppliers(request.getProductId()));
        // System.out.println();
        System.out.println(webClientService.getForecasts(request.getProductId(), Integer.parseInt(request.getHorizonDays()), request.getAsOfDate()));
        // DB

        return ollamaService.callOllama(products, forecasts);
        // return PlanResponse.builder().message(ollamaService.callOllama(products, forecasts)).build();
    }

}
