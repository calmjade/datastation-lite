package com.calmjade.datastation.service;

import com.calmjade.datastation.dto.TableMetaDto;
import com.calmjade.datastation.dto.TableMetaDto.ColumnInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaService {

    private record ManagedTable(String name, String label, String description) {
    }

    private static final List<ManagedTable> MANAGED_TABLES = List.of(
            new ManagedTable("biz_sales_orders", "销售订单明细表", "包含各区域、产品线及渠道的合同金额、毛利、订单日期与回款状态"),
            new ManagedTable("biz_channel_kpi", "渠道投放 KPI 月表", "按月统计各获客渠道的线索量、签约数、转化率、投放成本与 ROI"),
            new ManagedTable("ds_report_config", "低代码报表配置表", "存储已发布的 BI 报表 SQL、字段格式化规则及 ECharts 可视化配置")
    );

    private final JdbcTemplate jdbcTemplate;

    public SchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getAvailableTableNames() {
        return MANAGED_TABLES.stream().map(ManagedTable::name).toList();
    }

    public List<TableMetaDto> getAllTableMetadata() {
        List<TableMetaDto> result = new ArrayList<>();
        for (ManagedTable table : MANAGED_TABLES) {
            long rowCount = queryRowCount(table.name());
            List<ColumnInfo> columns = inspectColumnsAndSamples(table.name());
            result.add(new TableMetaDto(table.name(), table.label(), table.description(), rowCount, columns));
        }
        return result;
    }

    private long queryRowCount(String tableName) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return count != null ? count : 0L;
    }

    private List<ColumnInfo> inspectColumnsAndSamples(String tableName) {
        Map<String, Object> sampleRow = jdbcTemplate.query("SELECT * FROM " + tableName + " LIMIT 1", rs -> {
            Map<String, Object> map = new LinkedHashMap<>();
            ResultSetMetaData meta = rs.getMetaData();
            boolean hasRow = rs.next();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String colName = QueryService.normalizeColumnName(meta.getColumnLabel(i));
                String colType = meta.getColumnTypeName(i);
                Object sample = hasRow ? rs.getObject(i) : null;
                map.put(colName, new ColumnInfo(colName, colType, sample));
            }
            return map;
        });

        if (sampleRow == null) {
            return List.of();
        }
        List<ColumnInfo> cols = new ArrayList<>(sampleRow.size());
        for (Object val : sampleRow.values()) {
            cols.add((ColumnInfo) val);
        }
        return cols;
    }
}
