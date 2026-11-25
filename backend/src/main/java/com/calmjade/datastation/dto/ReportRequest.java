package com.calmjade.datastation.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportRequest(
        @NotBlank(message = "报表名称不能为空") String name,
        @NotBlank(message = "报表分类不能为空") String category,
        String description,
        @NotBlank(message = "SQL 查询语句不能为空") String sqlText,
        String columnsJson,
        String chartConfigJson,
        String transformScript
) {
}
