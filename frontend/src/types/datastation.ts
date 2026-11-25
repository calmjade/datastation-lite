export type ActiveTab = 'portal' | 'studio' | 'explorer';

export type ColumnFormatter = 'default' | 'number' | 'currency' | 'percent' | 'tag';

export type ChartType = 'bar' | 'line' | 'pie' | 'radar';

export type PaletteKey = 'business' | 'aurora' | 'emerald' | 'sunset';

export interface HealthStatus {
  status: string;
  service: string;
  reportCount: number;
  tables: string[];
}

export interface TableColumnInfo {
  name: string;
  type: string;
  sample: unknown;
}

export interface TableMeta {
  name: string;
  label: string;
  description: string;
  rowCount: number;
  columns: TableColumnInfo[];
}

export interface QueryColumnMeta {
  name: string;
  type: string;
}

export interface QueryExecuteResponse {
  columns: QueryColumnMeta[];
  rows: Record<string, unknown>[];
  rowCount: number;
  executionMs: number;
}

export interface ColumnConfig {
  key: string;
  label: string;
  formatter: ColumnFormatter;
}

export interface ChartConfig {
  chartType: ChartType;
  xField: string;
  yFields: string[];
  palette: PaletteKey;
  smooth: boolean;
  showLabel: boolean;
}

export interface ReportConfig {
  id: number;
  name: string;
  category: string;
  description: string;
  sqlText: string;
  columnsJson: string;
  chartConfigJson: string;
  transformScript: string;
  updatedAt: string;
}

export interface ReportRequest {
  name: string;
  category: string;
  description: string;
  sqlText: string;
  columnsJson: string;
  chartConfigJson: string;
  transformScript: string;
}

export interface ReportRunResponse {
  report: ReportConfig;
  columns: QueryColumnMeta[];
  rows: Record<string, unknown>[];
  rowCount: number;
  executionMs: number;
}

export interface PortalKpis {
  totalRevenue: number;
  totalProfit: number;
  totalOrders: number;
  avgRoi: number;
}
