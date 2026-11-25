package com.calmjade.datastation.dto;

import jakarta.validation.constraints.NotBlank;

public record QueryExecuteRequest(
        @NotBlank(message = "SQL 语句不能为空") String sql,
        Integer limit
) {
}
