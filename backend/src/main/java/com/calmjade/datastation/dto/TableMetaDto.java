package com.calmjade.datastation.dto;

import java.util.List;

public record TableMetaDto(
        String name,
        String label,
        String description,
        long rowCount,
        List<ColumnInfo> columns
) {
    public record ColumnInfo(String name, String type, Object sample) {
    }
}
