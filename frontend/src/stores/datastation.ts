import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  createReport,
  deleteReport,
  executeQuery,
  fetchHealth,
  fetchReports,
  fetchSchemaTables,
  runReport,
  updateReport,
} from '../api/client';
import type {
  ActiveTab,
  ChartConfig,
  ColumnConfig,
  HealthStatus,
  PortalKpis,
  QueryExecuteResponse,
  ReportConfig,
  ReportRequest,
  ReportRunResponse,
  TableMeta,
} from '../types/datastation';
import {
  inferDefaultFormatter,
  isNumericColumnType,
  parseChartConfig,
  parseColumnsConfig,
} from '../utils/formatters';

export interface StudioDraft {
  editingId: number | null;
  name: string;
  category: string;
  description: string;
  sqlText: string;
  columns: ColumnConfig[];
  chartConfig: ChartConfig;
  queryResult: QueryExecuteResponse | null;
  queryError: string | null;
  runningQuery: boolean;
  saving: boolean;
}

const DEFAULT_STUDIO_SQL = `SELECT region,
       COUNT(*) AS order_count,
       ROUND(SUM(amount), 2) AS total_amount,
       ROUND(SUM(profit), 2) AS total_profit,
       ROUND(SUM(profit) * 100.0 / SUM(amount), 2) AS margin_rate
FROM biz_sales_orders
GROUP BY region
ORDER BY total_amount DESC`;

function createBlankDraft(): StudioDraft {
  return {
    editingId: null,
    name: '自定义业务分析报表',
    category: '经营分析',
    description: '基于只读 SQL 实时聚合业务指标并生成可视化看板',
    sqlText: DEFAULT_STUDIO_SQL,
    columns: [
      { key: 'region', label: '销售区域', formatter: 'tag' },
      { key: 'order_count', label: '订单数', formatter: 'number' },
      { key: 'total_amount', label: '总营收', formatter: 'currency' },
      { key: 'total_profit', label: '总毛利', formatter: 'currency' },
      { key: 'margin_rate', label: '毛利率', formatter: 'percent' },
    ],
    chartConfig: {
      chartType: 'bar',
      xField: 'region',
      yFields: ['total_amount', 'total_profit'],
      palette: 'business',
      smooth: true,
      showLabel: false,
    },
    queryResult: null,
    queryError: null,
    runningQuery: false,
    saving: false,
  };
}

export const useDataStationStore = defineStore('datastation', () => {
  const activeTab = ref<ActiveTab>('portal');
  const health = ref<HealthStatus | null>(null);
  const tables = ref<TableMeta[]>([]);
  const reports = ref<ReportConfig[]>([]);
  const reportResults = ref<Record<number, ReportRunResponse>>({});
  const reportLoading = ref<Record<number, boolean>>({});
  const reportErrors = ref<Record<number, string>>({});
  const portalKpis = ref<PortalKpis | null>(null);
  const portalBusy = ref(false);
  const studioDraft = ref<StudioDraft>(createBlankDraft());

  async function loadHealthAndSchema() {
    const [h, t] = await Promise.all([fetchHealth(), fetchSchemaTables()]);
    health.value = h;
    tables.value = t;
  }

  async function loadPortalKpis() {
    try {
      const [salesRes, roiRes] = await Promise.all([
        executeQuery(
          'SELECT COUNT(*) AS order_count, COALESCE(SUM(amount), 0) AS total_revenue, COALESCE(SUM(profit), 0) AS total_profit FROM biz_sales_orders',
          1
        ),
        executeQuery(
          'SELECT COALESCE(ROUND(AVG(roi), 2), 0) AS avg_roi FROM biz_channel_kpi',
          1
        ),
      ]);
      const salesRow = salesRes.rows[0] || {};
      const roiRow = roiRes.rows[0] || {};
      portalKpis.value = {
        totalRevenue: Number(salesRow.total_revenue ?? 0),
        totalProfit: Number(salesRow.total_profit ?? 0),
        totalOrders: Number(salesRow.order_count ?? 0),
        avgRoi: Number(roiRow.avg_roi ?? 0),
      };
    } catch {
      // Keep previous KPIs if query temporarily fails
    }
  }

  async function runSingleReport(id: number) {
    reportLoading.value[id] = true;
    delete reportErrors.value[id];
    try {
      const res = await runReport(id);
      reportResults.value[id] = res;
    } catch (err) {
      reportErrors.value[id] = err instanceof Error ? err.message : '报表执行失败';
    } finally {
      reportLoading.value[id] = false;
    }
  }

  async function refreshPortal() {
    portalBusy.value = true;
    try {
      const [reportList] = await Promise.all([
        fetchReports(),
        loadHealthAndSchema(),
        loadPortalKpis(),
      ]);
      reports.value = reportList;
      await Promise.all(reportList.map((r) => runSingleReport(r.id)));
    } finally {
      portalBusy.value = false;
    }
  }

  async function initApp() {
    await refreshPortal();
    if (!studioDraft.value.queryResult) {
      await runStudioQuery();
    }
  }

  async function removeReport(id: number) {
    await deleteReport(id);
    reports.value = reports.value.filter((r) => r.id !== id);
    delete reportResults.value[id];
    delete reportErrors.value[id];
    if (studioDraft.value.editingId === id) {
      studioDraft.value.editingId = null;
    }
    await loadHealthAndSchema();
  }

  async function runStudioQuery() {
    const draft = studioDraft.value;
    draft.runningQuery = true;
    draft.queryError = null;
    try {
      const res = await executeQuery(draft.sqlText, 200);
      draft.queryResult = res;

      // Merge columns: preserve user-defined labels & formatters for existing keys
      const existingByKey = new Map(draft.columns.map((c) => [c.key, c]));
      draft.columns = res.columns.map((col) => {
        const prev = existingByKey.get(col.name);
        if (prev) return prev;
        return {
          key: col.name,
          label: col.name,
          formatter: inferDefaultFormatter(col.name, col.type),
        };
      });

      const validKeys = new Set(res.columns.map((c) => c.name));
      const numericCols = res.columns
        .filter((c) => isNumericColumnType(c.type))
        .map((c) => c.name);
      const nonNumericCols = res.columns
        .filter((c) => !isNumericColumnType(c.type))
        .map((c) => c.name);

      if (!draft.chartConfig.xField || !validKeys.has(draft.chartConfig.xField)) {
        draft.chartConfig.xField = nonNumericCols[0] || res.columns[0]?.name || '';
      }

      const validY = draft.chartConfig.yFields.filter((f) => validKeys.has(f));
      if (validY.length > 0) {
        draft.chartConfig.yFields = validY;
      } else {
        const candidates = numericCols.filter((c) => c !== draft.chartConfig.xField);
        draft.chartConfig.yFields = candidates.slice(0, 2);
      }
    } catch (err) {
      draft.queryError = err instanceof Error ? err.message : 'SQL 执行失败';
    } finally {
      draft.runningQuery = false;
    }
  }

  async function openInStudio(report?: ReportConfig) {
    if (!report) {
      studioDraft.value = createBlankDraft();
      activeTab.value = 'studio';
      await runStudioQuery();
      return;
    }

    const parsedCols = parseColumnsConfig(report.columnsJson);
    const parsedChart = parseChartConfig(report.chartConfigJson);

    studioDraft.value = {
      editingId: report.id,
      name: report.name,
      category: report.category,
      description: report.description || '',
      sqlText: report.sqlText,
      columns: parsedCols,
      chartConfig: parsedChart,
      queryResult: null,
      queryError: null,
      runningQuery: false,
      saving: false,
    };
    activeTab.value = 'studio';
    await runStudioQuery();
  }

  async function generateSelectToStudio(table: TableMeta, customSql?: string) {
    const colList = table.columns
      .slice(0, 8)
      .map((c) => c.name)
      .join(', ');
    const sql =
      customSql ||
      `SELECT ${colList || '*'}\nFROM ${table.name}\nLIMIT 50`;

    studioDraft.value = {
      editingId: null,
      name: `${table.label}探索分析`,
      category: '自定义分析',
      description: `基于 ${table.name} 生成的只读数据查询报表`,
      sqlText: sql,
      columns: [],
      chartConfig: {
        chartType: 'bar',
        xField: '',
        yFields: [],
        palette: 'business',
        smooth: true,
        showLabel: false,
      },
      queryResult: null,
      queryError: null,
      runningQuery: false,
      saving: false,
    };
    activeTab.value = 'studio';
    await runStudioQuery();
  }

  async function saveStudioReport(asNew = false): Promise<ReportConfig> {
    const draft = studioDraft.value;
    draft.saving = true;
    try {
      const payload: ReportRequest = {
        name: draft.name.trim(),
        category: draft.category.trim() || '自定义分析',
        description: draft.description.trim(),
        sqlText: draft.sqlText.trim(),
        columnsJson: JSON.stringify(draft.columns),
        chartConfigJson: JSON.stringify(draft.chartConfig),
        transformScript: '',
      };

      const saved =
        draft.editingId && !asNew
          ? await updateReport(draft.editingId, payload)
          : await createReport(payload);

      draft.editingId = saved.id;

      // Sync portal state
      const [reportList] = await Promise.all([fetchReports(), loadHealthAndSchema()]);
      reports.value = reportList;
      await runSingleReport(saved.id);
      return saved;
    } finally {
      draft.saving = false;
    }
  }

  return {
    activeTab,
    health,
    tables,
    reports,
    reportResults,
    reportLoading,
    reportErrors,
    portalKpis,
    portalBusy,
    studioDraft,
    initApp,
    refreshPortal,
    runSingleReport,
    removeReport,
    runStudioQuery,
    openInStudio,
    generateSelectToStudio,
    saveStudioReport,
  };
});
