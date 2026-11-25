package com.calmjade.datastation.dto;

import com.calmjade.datastation.entity.ReportConfig;

import java.util.List;
import java.util.Map;

public record ReportRunResponse(
        ReportConfig report,
        List<QueryExecuteResponse.ColumnMeta> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        long executionMs
) {
}
