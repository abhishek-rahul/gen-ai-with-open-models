package com.javaone.openmodels.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolCatalog {

    public List<ToolSchema> schemas() {
        return List.of(
            new ToolSchema(
                "getOrderStatus",
                "Get current order status, delivery date, and tracking information by order id.",
                List.of(new ToolParameter("orderId", "string", "Order id, for example ORD-1001", true))
            ),
            new ToolSchema(
                "getRefundPolicy",
                "Find refund policy by purchase category.",
                List.of(new ToolParameter("category", "string", "software, workshop, or merchandise", true))
            ),
            new ToolSchema(
                "getWeather",
                "Get a simulated weather API report for demo cities.",
                List.of(new ToolParameter("city", "string", "Bangalore, Delhi, or San Francisco", true))
            )
        );
    }

    public record ToolSchema(String name, String description, List<ToolParameter> parameters) {
    }

    public record ToolParameter(String name, String type, String description, boolean required) {
    }
}
