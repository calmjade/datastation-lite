package com.calmjade.datastation.controller;

import com.calmjade.datastation.service.ReportService;
import com.calmjade.datastation.service.SchemaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final ReportService reportService;
    private final SchemaService schemaService;

    public HealthController(ReportService reportService, SchemaService schemaService) {
        this.reportService = reportService;
        this.schemaService = schemaService;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "datastation-lite");
        body.put("reportCount", reportService.countReports());
        body.put("tables", schemaService.getAvailableTableNames());
        return body;
    }
}
