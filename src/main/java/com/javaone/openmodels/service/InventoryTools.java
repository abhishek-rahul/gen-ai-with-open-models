package com.javaone.openmodels.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Map;

public class InventoryTools {

    @Tool("Look up current stock level for a product by SKU")
    public String checkStock(@P("Product SKU") String sku) {
        // Simulated inventory lookup
        Map<String, Integer> inventory = Map.of(
            "JDK-21", 150, "MVN-4", 89, "GRAAL-22", 12
        );
        Integer qty = inventory.get(sku);
        return qty != null
            ? "SKU %s: %d units in stock".formatted(sku, qty)
            : "SKU %s: not found in inventory".formatted(sku);
    }
}
