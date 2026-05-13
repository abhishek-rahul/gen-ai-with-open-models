package com.javaone.openmodels.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ToolAuditLog {

    private final CopyOnWriteArrayList<ToolAuditEntry> entries = new CopyOnWriteArrayList<>();

    public void success(String toolName, String arguments, String result) {
        entries.add(new ToolAuditEntry(
            UUID.randomUUID().toString(),
            Instant.now(),
            toolName,
            arguments,
            "SUCCESS",
            result
        ));
    }

    public void failure(String toolName, String arguments, String error) {
        entries.add(new ToolAuditEntry(
            UUID.randomUUID().toString(),
            Instant.now(),
            toolName,
            arguments,
            "ERROR",
            error
        ));
    }

    public List<ToolAuditEntry> entries() {
        return entries.reversed();
    }

    public void clear() {
        entries.clear();
    }

    public record ToolAuditEntry(
        String id,
        Instant timestamp,
        String toolName,
        String arguments,
        String status,
        String result
    ) {
    }
}
