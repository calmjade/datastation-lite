package com.calmjade.datastation.dto;

import java.util.List;
import java.util.Map;

public record QueryExecuteResponse(
        List<ColumnMeta> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        long executionMs
) {
    public record ColumnMeta(String name, String type) {
    }
}
