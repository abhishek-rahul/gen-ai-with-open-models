package com.javaone.openmodels.dto;

import java.util.List;

public record GeneratedTicket(
    String title,
    String description,
    String priority,
    String category,
    List<String> tags
) {
}
