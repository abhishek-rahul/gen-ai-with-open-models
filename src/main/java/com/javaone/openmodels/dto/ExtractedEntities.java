package com.javaone.openmodels.dto;

import java.util.List;

public record ExtractedEntities(
    List<String> people,
    List<String> organizations,
    List<String> products,
    List<String> technologies,
    List<String> dates,
    String summary
) {
}
