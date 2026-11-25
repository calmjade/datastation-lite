package com.calmjade.datastation.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates that user-supplied SQL is strictly a single read-only SELECT or WITH query.
 */
@Component
public class SqlSecurityValidator {

    private static final List<String> FORBIDDEN_KEYWORDS = List.of(
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "TRUNCATE",
            "CREATE", "MERGE", "EXEC", "EXECUTE", "CALL", "GRANT",
            "REVOKE", "REPLACE", "RUNSCRIPT", "SCRIPT", "SHUTDOWN"
    );

    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "\\b(" + String.join("|", FORBIDDEN_KEYWORDS) + ")\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ALLOWED_START_PATTERN = Pattern.compile(
            "^(SELECT|WITH)(\\s+|\\()",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Validates the input SQL and returns a sanitized single-statement SQL string ready for execution.
     */
    public String validateAndSanitize(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            throw new SqlValidationException("SQL 语句不能为空");
        }

        String cleanInput = rawSql.trim();
        if (cleanInput.endsWith(";")) {
            cleanInput = cleanInput.substring(0, cleanInput.length() - 1).trim();
        }

        String stripped = stripCommentsAndStrings(cleanInput);
        String trimmedForCheck = stripped.trim();

        if (trimmedForCheck.isEmpty()) {
            throw new SqlValidationException("SQL 语句不能为空");
        }

        // Reject any semicolon remaining in executable SQL (multi-statement injection)
        if (stripped.indexOf(';') >= 0) {
            throw new SqlValidationException("安全拦截：禁止使用分号执行多条 SQL 语句");
        }

        // Ensure first token is strictly SELECT or WITH (supports multi-line SQL)
        if (!ALLOWED_START_PATTERN.matcher(trimmedForCheck).find()) {
            throw new SqlValidationException("安全拦截：仅允许执行只读 SELECT 或 WITH 查询语句");
        }

        Matcher matcher = FORBIDDEN_PATTERN.matcher(trimmedForCheck);
        if (matcher.find()) {
            String keyword = matcher.group(1).toUpperCase(Locale.ROOT);
            throw new SqlValidationException("安全拦截：只读查询禁止包含危险关键字 [" + keyword + "]");
        }

        return cleanInput;
    }

    /**
     * Replaces SQL string literals and comments with spaces so keyword/semicolon checks
     * only inspect executable SQL tokens while preserving token boundaries.
     */
    private String stripCommentsAndStrings(String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        int len = sql.length();
        int i = 0;

        while (i < len) {
            char c = sql.charAt(i);
            char next = (i + 1 < len) ? sql.charAt(i + 1) : '\0';

            // Single-line comment --
            if (c == '-' && next == '-') {
                sb.append(' ');
                i += 2;
                while (i < len && sql.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }

            // Block comment /* ... */
            if (c == '/' && next == '*') {
                sb.append(' ');
                i += 2;
                while (i + 1 < len && !(sql.charAt(i) == '*' && sql.charAt(i + 1) == '/')) {
                    i++;
                }
                i += 2;
                continue;
            }

            // Single-quoted string literal '...'
            if (c == '\'') {
                sb.append(' ');
                i++;
                while (i < len) {
                    if (sql.charAt(i) == '\'') {
                        if (i + 1 < len && sql.charAt(i + 1) == '\'') {
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }
                    i++;
                }
                sb.append(' ');
                continue;
            }

            // Double-quoted identifier "..."
            if (c == '"') {
                sb.append(' ');
                i++;
                while (i < len) {
                    if (sql.charAt(i) == '"') {
                        if (i + 1 < len && sql.charAt(i + 1) == '"') {
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }
                    i++;
                }
                sb.append(' ');
                continue;
            }

            sb.append(c);
            i++;
        }

        return sb.toString();
    }
}
