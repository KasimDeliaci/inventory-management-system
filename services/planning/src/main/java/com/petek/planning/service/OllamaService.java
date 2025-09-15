package com.petek.planning.service;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OllamaService {

    private final OllamaChatModel chatModel;

    public String callOllama(String products, String forecasts) {
        String prompt = """
        ## System Prompt

        You are a replenishment planner. Use only the provided facts JSON. If a field is missing or null, state the assumption and use a safe fallback (e.g., avgLeadTimeDays=5). Keep responses under **120 tokens**. Do not invent numbers. If data is insufficient, propose a next action.

        For countable UoMs {adet, koli, paket, çuval, şişe} round orderQty to the nearest integer multiple of minOrderQuantity; otherwise round to 3 decimals. Consider all suppliers and pick one; explain briefly if the preferred supplier is not chosen. Return exactly one JSON object, with no extra text.

        Use sumYhat as the primary demand signal, regardless of forecast horizonDays. The optional daily forecast values are only for observing potential demand spikes, not for calculating the primary order quantity.

        -----

        ## Output Schema

        The model must return a single JSON object with the following structure:

        {
          "text": "string",
          "recommendation": {
            "supplierId": "number|null",
            "orderQty": "number",
            "orderDate": "YYYY-MM-DD",
            "confidence": "integer (0-100)",
            "assumptions": ["string"],
            "risks": ["string"]
          },
          "facts": {
            "productId": "int",
            "forecastId": "int",
            "asOfDate": "YYYY-MM-DD",
            "horizonDays": "int",
            "sumYhat": "number",
            "available": "number",
            "safetyStock": "number",
            "reorderPoint": "number",
            "avgDailyDemand": "number",
            "daysToSafety": "number",
            "targetArrivalDate": "YYYY-MM-DD",
            "chosenSupplierId": "number|null",
            "baselineOrderQty": "number",
            "baselineOrderDate": "YYYY-MM-DD",
            "supplierOptions": [
              {
                "supplierId": "int",
                "supplierName": "string",
                "isPreferred": "bool",
                "active": "bool",
                "minOrderQuantity": "number",
                "avgLeadTimeDays": "number",
                "lastDeliveryDate": "YYYY-MM-DD|null"
              }
            ]
          }
        }

        -----

        ## User Prompt

        Facts JSON:
        Products: %s
        Forecasts: %s
        """.formatted(products, forecasts);

        ChatResponse response = chatModel.call(
            new Prompt(
                prompt,
                ChatOptions.builder()
                    .maxTokens(120)  // limit output to 120 tokens
                    .build()
            )
        );

        return response.getResult().getOutput().getText();
    }
}
