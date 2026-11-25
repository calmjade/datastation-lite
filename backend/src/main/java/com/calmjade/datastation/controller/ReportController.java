package com.calmjade.datastation.controller;

import com.calmjade.datastation.dto.ReportRequest;
import com.calmjade.datastation.dto.ReportRunResponse;
import com.calmjade.datastation.entity.ReportConfig;
import com.calmjade.datastation.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ReportConfig> listReports() {
        return reportService.findAll();
    }

    @GetMapping("/{id}")
    public ReportConfig getReport(@PathVariable Long id) {
        return reportService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportConfig createReport(@Valid @RequestBody ReportRequest request) {
        return reportService.create(request);
    }

    @PutMapping("/{id}")
    public ReportConfig updateReport(@PathVariable Long id, @Valid @RequestBody ReportRequest request) {
        return reportService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReport(@PathVariable Long id) {
        reportService.delete(id);
    }

    @PostMapping("/{id}/run")
    public ReportRunResponse runReport(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Integer limit = null;
        if (body != null && body.get("limit") instanceof Number num) {
            limit = num.intValue();
        }
        return reportService.runReport(id, limit);
    }
}
