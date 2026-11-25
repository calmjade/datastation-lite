package com.calmjade.datastation.service;

import com.calmjade.datastation.dto.QueryExecuteResponse;
import com.calmjade.datastation.dto.QueryExecuteResponse.ColumnMeta;
import com.calmjade.datastation.security.SqlSecurityValidator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class QueryService {

    public static final int MAX_ROW_LIMIT = 500;
    public static final int DEFAULT_ROW_LIMIT = 200;

    private final JdbcTemplate jdbcTemplate;
    private final SqlSecurityValidator sqlSecurityValidator;

    public QueryService(JdbcTemplate jdbcTemplate, SqlSecurityValidator sqlSecurityValidator) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlSecurityValidator = sqlSecurityValidator;
    }

    public QueryExecuteResponse executeReadOnlyQuery(String rawSql, Integer requestedLimit) {
        String safeSql = sqlSecurityValidator.validateAndSanitize(rawSql);
        int effectiveLimit = normalizeLimit(requestedLimit);

        long startNanos = System.nanoTime();

        List<ColumnMeta> columns = new ArrayList<>();
        List<Map<String, Object>> rows = jdbcTemplate.query(con -> {
            var ps = con.prepareStatement(safeSql);
            ps.setMaxRows(effectiveLimit);
            return ps;
        }, rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int colCount = metaData.getColumnCount();
            List<String> columnNames = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                String label = normalizeColumnName(metaData.getColumnLabel(i));
                String type = metaData.getColumnTypeName(i);
                columns.add(new ColumnMeta(label, type));
                columnNames.add(label);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            while (rs.next() && result.size() < effectiveLimit) {
                Map<String, Object> row = new LinkedHashMap<>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    row.put(columnNames.get(i - 1), convertValue(rs.getObject(i)));
                }
                result.add(row);
            }
            return result;
        });

        long executionMs = Math.max(1L, (System.nanoTime() - startNanos) / 1_000_000L);
        List<Map<String, Object>> safeRows = rows != null ? rows : List.of();
        return new QueryExecuteResponse(columns, safeRows, safeRows.size(), executionMs);
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_ROW_LIMIT;
        }
        return Math.min(requestedLimit, MAX_ROW_LIMIT);
    }

    static String normalizeColumnName(String rawName) {
        if (rawName == null) {
            return "col";
        }
        if (rawName.equals(rawName.toUpperCase(Locale.ROOT))) {
            return rawName.toLowerCase(Locale.ROOT);
        }
        return rawName;
    }

    private Object convertValue(Object value) throws SQLException {
        if (value instanceof java.sql.Clob clob) {
            long length = clob.length();
            return length > 0 ? clob.getSubString(1, (int) Math.min(length, 100_000)) : "";
        }
        return value;
    }
}
