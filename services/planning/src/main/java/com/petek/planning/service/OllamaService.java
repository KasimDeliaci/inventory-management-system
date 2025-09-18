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
  
    public String callOllama(String product, String stock, String suppliers, String forecasts) {
        String prompt = """
        ## System Prompt

        You are an inventory replenishment planner. **Use only provided JSON data.** If field missing, use safe fallback (e.g., `avgLeadTimeDays=5`). But do not invent numbers **Respond in Turkish.**

        ## CRITICAL RULES

        ### Order Criteria (ALWAYS CHECK BOTH)
        Place order ONLY if either condition is true:
        1. `quantityAvailable < reorderPoint` 
        2. `quantityAvailable < sum + safetyStock`

        **If neither condition is met, DO NOT ORDER.**

        ### Calculations
        - Demand = `sum` (sumYhat) value
        - orderQty = `max(0, sum + safetyStock - quantityAvailable)` use this result in your final response for recommending
        - Round UP to MOQ
        - Countable units {adet, koli, paket, çuval, şişe} = integers only (no decimals)

        ### Supplier Selection
        **Consider all suppliers and pick one**; explain briefly if the preferred supplier is not chosen.
        1. Priority: Active + Preferred supplier
        2. Otherwise: Active supplier with shortest `avgLeadTimeDays`
        3. Tiebreaker: Most recent `lastDeliveryDate` or lower MOQ

        ### Response Format (3-4 sentences, plain text, Turkish)
        1. Period forecast (7 days="bu hafta", 14="iki hafta"): "~X birim"
        2. Decision: "X adet sipariş verin..." OR "Stok yeterli, sipariş gerekmiyor..."
        3. Supplier + lead time
        4. (Optional) Mention spike if `daily` shows sudden increase
        5. **DO NOT use backslash and asterisk like characters. Response must be plain text only.**

        ## Example Output

        **No Order Needed:**
        "<productName> için bu haftaki satış öngörüsü yaklaşık <sum> adet. Mevcut <quantityAvailable> adet stok, <reorderPoint>t'in üzerinde olduğundan dolayı şu anda sipariş vermeye gerek yok. Stok seviyelerini yakından takip edin, özellikle de hafta sonu satışları artabilir."

        **Order Required:**
        "<productName> için bu haftaki satış öngörüsü ~<sum> adettir. Mevcut <quantityAvailable> adetlik stoku korumak amacıyla, tercihli tedarikçimiz <supplierName>'den minimum sipariş miktarı <minOrderQuantity> gereği <orderQty> adetlik (3x<quantityAvailable>) sipariş verilmelidir.

        Lütfen <avgLeadTimeDays> teslimat süresini dikkate alın. Hafta sonu bu ürünün satışları artabileceğinden rakamları yakından takip edin."

        ## Data Input Order
        1. Product info JSON: %s
        2. Current stock JSON: %s
        3. Supplier info JSON: %s
        4. Forecast response JSON: %s
        """.formatted(product, stock, suppliers, forecasts);

        ChatResponse response = chatModel.call(
            new Prompt(
                prompt,
                ChatOptions.builder()
                    .temperature(0.0)
                    .build()
            )
        );

        return response.getResult().getOutput().getText();
    }
}
