package com.javaone.openmodels.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InventoryTools {

    private final Map<String, Integer> inventory = Map.of(
        "JDK-21", 150,
        "MVN-4", 89,
        "GRAAL-22", 12,
        "jdk-21", 151
    );

    @Tool(description = "Look up the current stock level for a product by SKU")
    public String checkStock(@ToolParam(description = "Product SKU") String sku) {
        Integer qty = inventory.get(sku);
        return qty != null
            ? "SKU %s: %d units in stock".formatted(sku, qty)
            : "SKU %s: not found in inventory".formatted(sku);
    }
}
