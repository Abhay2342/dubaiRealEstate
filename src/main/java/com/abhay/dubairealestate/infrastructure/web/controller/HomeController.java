package com.abhay.dubairealestate.infrastructure.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("application", "Dubai Real Estate API");
        info.put("description", "REST API for Dubai DLD property sales and rent transactions");
        info.put("endpoints", Map.of(
                "sales", "/api/sales",
                "rents", "/api/rents",
                "mcp-sse",     "/sse",
                "mcp-message", "/mcp/message"
        ));
        return ResponseEntity.ok(info);
    }
}
