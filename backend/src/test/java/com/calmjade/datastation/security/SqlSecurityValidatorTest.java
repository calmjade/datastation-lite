package com.calmjade.datastation.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlSecurityValidatorTest {

    private final SqlSecurityValidator validator = new SqlSecurityValidator();

    @Test
    void allowsValidReadOnlySelectAndWithQueries() {
        assertDoesNotThrow(() -> validator.validateAndSanitize(
                "SELECT region, SUM(amount) AS total FROM biz_sales_orders GROUP BY region"
        ));
        assertDoesNotThrow(() -> validator.validateAndSanitize(
                "WITH cte AS (\n  SELECT region, amount FROM biz_sales_orders\n)\nSELECT * FROM cte;"
        ));
        assertDoesNotThrow(() -> validator.validateAndSanitize(
                "SELECT * FROM biz_sales_orders WHERE status = 'DROP TABLE'"
        ));
    }

    @Test
    void blocksDmlDdlAndMultiStatementInjection() {
        assertThrows(SqlValidationException.class,
                () -> validator.validateAndSanitize("DROP TABLE biz_sales_orders"));
        assertThrows(SqlValidationException.class,
                () -> validator.validateAndSanitize("SELECT 1; DELETE FROM ds_report_config"));
        assertThrows(SqlValidationException.class,
                () -> validator.validateAndSanitize("CREATE ALIAS EXEC_CMD AS 'void f(){}'"));
    }

    @Test
    void blocksH2DangerousFunctionsQuotedBypassAndSystemSchemas() {
        assertThrows(SqlValidationException.class,
                () -> validator.validateAndSanitize("SELECT FILE_READ('/etc/passwd')"));
        assertThrows(SqlValidationException.class,
                () -> validator.validateAndSanitize("SELECT \"FILE_READ\"('/etc/passwd')"));
        assertThrows(SqlValidationException.class,
                () -> validator.validateAndSanitize("SELECT * FROM CSVREAD('/etc/hosts')"));
        assertThrows(SqlValidationException.class,
                () -> validator.validateAndSanitize("SELECT * FROM LINK_SCHEMA('test', '', 'jdbc:h2:mem:x', '', '', '')"));
        assertThrows(SqlValidationException.class,
                () -> validator.validateAndSanitize("SELECT * FROM INFORMATION_SCHEMA.TABLES"));
    }
}
