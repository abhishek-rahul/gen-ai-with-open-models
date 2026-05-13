package com.javaone.openmodels.service;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class CommerceSupportTools {

    private final ToolAuditLog auditLog;

    private final Map<String, OrderStatus> orders = Map.of(
        "ORD-1001", new OrderStatus("ORD-1001", "SHIPPED", "TRK-JAVA-1001", "2026-05-15", "JDK 21 Hoodie"),
        "ORD-1002", new OrderStatus("ORD-1002", "PROCESSING", "Not assigned yet", "2026-05-18", "LangChain4j Workshop Pass"),
        "ORD-1003", new OrderStatus("ORD-1003", "DELIVERED", "TRK-AI-1003", "2026-05-10", "Open Models Book")
    );

    private final Map<String, String> refundPolicies = Map.of(
        "software", "Software purchases are refundable within 14 days if the license key has not been activated.",
        "workshop", "Workshop passes are refundable up to 7 days before the session. Transfers are allowed until 24 hours before the session.",
        "merchandise", "Merchandise can be returned within 30 days if unused and in original packaging."
    );

    private final Map<String, WeatherReport> weather = Map.of(
        "bangalore", new WeatherReport("Bangalore", "29 C", "Partly cloudy", "Carry water; afternoon traffic may be slow."),
        "delhi", new WeatherReport("Delhi", "35 C", "Hot and dry", "Avoid outdoor demos in peak afternoon heat."),
        "san francisco", new WeatherReport("San Francisco", "16 C", "Cool and windy", "Bring a light jacket for the evening.")
    );

    public CommerceSupportTools(ToolAuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Tool("Get current order status, delivery date, and tracking information by order id")
    public String getOrderStatus(@P("Order id, for example ORD-1001") String orderId) {
        String normalizedOrderId = normalize(orderId);
        String arguments = "orderId=%s".formatted(normalizedOrderId);

        if (normalizedOrderId.isBlank()) {
            return fail("getOrderStatus", arguments, "Order id is required. Ask the user for an order id like ORD-1001.");
        }

        OrderStatus order = orders.get(normalizedOrderId);
        if (order == null) {
            return fail("getOrderStatus", arguments, "No order found for %s. Ask the user to verify the order id.".formatted(normalizedOrderId));
        }

        String result = "Order %s is %s. Item: %s. Tracking: %s. Estimated delivery: %s."
            .formatted(order.orderId(), order.status(), order.item(), order.trackingNumber(), order.estimatedDelivery());
        auditLog.success("getOrderStatus", arguments, result);
        return result;
    }

    @Tool("Find refund policy by purchase category such as software, workshop, or merchandise")
    public String getRefundPolicy(@P("Purchase category: software, workshop, or merchandise") String category) {
        String normalizedCategory = normalize(category).toLowerCase(Locale.ROOT);
        String arguments = "category=%s".formatted(normalizedCategory);

        if (normalizedCategory.isBlank()) {
            return fail("getRefundPolicy", arguments, "Purchase category is required. Supported values: software, workshop, merchandise.");
        }

        String policy = refundPolicies.get(normalizedCategory);
        if (policy == null) {
            return fail("getRefundPolicy", arguments, "Unsupported category '%s'. Supported values: software, workshop, merchandise.".formatted(normalizedCategory));
        }

        auditLog.success("getRefundPolicy", arguments, policy);
        return policy;
    }

    @Tool("Get a simulated weather API report for demo cities")
    public String getWeather(@P("City name, for example Bangalore, Delhi, or San Francisco") String city) {
        String normalizedCity = normalize(city).toLowerCase(Locale.ROOT);
        String arguments = "city=%s".formatted(normalizedCity);

        if (normalizedCity.isBlank()) {
            return fail("getWeather", arguments, "City is required. Supported demo cities: Bangalore, Delhi, San Francisco.");
        }

        WeatherReport report = weather.get(normalizedCity);
        if (report == null) {
            return fail("getWeather", arguments, "Weather API has no demo data for '%s'. Supported demo cities: Bangalore, Delhi, San Francisco.".formatted(city));
        }

        String result = "Weather for %s: %s, %s. Advice: %s"
            .formatted(report.city(), report.temperature(), report.condition(), report.advice());
        auditLog.success("getWeather", arguments, result);
        return result;
    }

    private String fail(String toolName, String arguments, String error) {
        auditLog.failure(toolName, arguments, error);
        return "TOOL_ERROR: %s".formatted(error);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record OrderStatus(
        String orderId,
        String status,
        String trackingNumber,
        String estimatedDelivery,
        String item
    ) {
    }

    private record WeatherReport(
        String city,
        String temperature,
        String condition,
        String advice
    ) {
    }
}
