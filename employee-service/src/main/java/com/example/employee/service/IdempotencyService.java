package com.example.employee.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class IdempotencyService {

    // In production, this would be Redis or a database table
    private final ConcurrentMap<String, Object> idempotencyStore = new ConcurrentHashMap<>();

    public boolean isProcessed(String key) {
        return idempotencyStore.containsKey(key);
    }

    public void storeResult(String key, Object result) {
        idempotencyStore.put(key, result);
    }

    public Object getResult(String key) {
        return idempotencyStore.get(key);
    }

    // Optional: cleanup old entries to prevent memory leaks
    public void removeResult(String key) {
        idempotencyStore.remove(key);
    }
}