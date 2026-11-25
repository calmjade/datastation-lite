import type {
  ChartConfig,
  ColumnConfig,
  ColumnFormatter,
  PaletteKey,
  QueryColumnMeta,
} from '../types/datastation';

export interface PaletteDefinition {
  key: PaletteKey;
  label: string;
  colors: string[];
}

export const PALETTES: Record<PaletteKey, PaletteDefinition> = {
  business: {
    key: 'business',
    label: '商务蓝紫',
    colors: ['#2563EB', '#4F46E5', '#0EA5E9', '#6366F1', '#14B8A6', '#F59E0B'],
  },
  aurora: {
    key: 'aurora',
    label: '极光青蓝',
    colors: ['#0284C7', '#06B6D4', '#10B981', '#3B82F6', '#8B5CF6', '#EC4899'],
  },
  emerald: {
    key: 'emerald',
    label: '翡翠森绿',
    colors: ['#059669', '#10B981', '#0D9488', '#34D399', '#2563EB', '#F59E0B'],
  },
  sunset: {
    key: 'sunset',
    label: '琥珀晚霞',
    colors: ['#EA580C', '#F59E0B', '#E11D48', '#8B5CF6', '#2563EB', '#10B981'],
  },
};

export const FORMATTER_OPTIONS: { value: ColumnFormatter; label: string }[] = [
  { value: 'default', label: '默认文本' },
  { value: 'number', label: '数值千分位' },
  { value: 'currency', label: '金额 (¥)' },
  { value: 'percent', label: '百分比 (%)' },
  { value: 'tag', label: '状态标签' },
];

export function inferDefaultFormatter(colName: string, colType?: string): ColumnFormatter {
  const lower = colName.toLowerCase();
  if (/(amount|profit|revenue|cost|price|fee)/.test(lower)) {
    return 'currency';
  }
  if (/(rate|ratio|pct|margin|percent|conversion)/.test(lower)) {
    return 'percent';
  }
  if (/(status|region|channel|product_line|category)/.test(lower)) {
    return 'tag';
  }
  const upperType = (colType || '').toUpperCase();
  if (/(INT|BIGINT|DECIMAL|NUMERIC|DOUBLE|FLOAT|REAL)/.test(upperType)) {
    return 'number';
  }
  return 'default';
}

export function isNumericColumnType(colType?: string): boolean {
  const upperType = (colType || '').toUpperCase();
  return /(INT|BIGINT|SMALLINT|TINYINT|DECIMAL|NUMERIC|DOUBLE|FLOAT|REAL|NUMBER)/.test(upperType);
}

export function parseColumnsConfig(
  columnsJson: string | null | undefined,
  fallbackColumns: QueryColumnMeta[] = []
): ColumnConfig[] {
  let parsed: ColumnConfig[] = [];
  if (columnsJson) {
    try {
      const raw = JSON.parse(columnsJson);
      if (Array.isArray(raw)) {
        parsed = raw
          .filter((item) => item && typeof item.key === 'string')
          .map((item) => ({
            key: item.key,
            label: typeof item.label === 'string' && item.label.trim() ? item.label : item.key,
            formatter: isValidFormatter(item.formatter) ? item.formatter : 'default',
          }));
      }
    } catch {
      parsed = [];
    }
  }

  if (fallbackColumns.length === 0) {
    return parsed;
  }

  const byKey = new Map(parsed.map((c) => [c.key, c]));
  return fallbackColumns.map((col) => {
    const existing = byKey.get(col.name);
    if (existing) {
      return existing;
    }
    return {
      key: col.name,
      label: col.name,
      formatter: inferDefaultFormatter(col.name, col.type),
    };
  });
}

function isValidFormatter(val: unknown): val is ColumnFormatter {
  return val === 'default' || val === 'number' || val === 'currency' || val === 'percent' || val === 'tag';
}

export function parseChartConfig(chartConfigJson: string | null | undefined): ChartConfig {
  const defaults: ChartConfig = {
    chartType: 'bar',
    xField: '',
    yFields: [],
    palette: 'business',
    smooth: true,
    showLabel: false,
  };

  if (!chartConfigJson) {
    return defaults;
  }

  try {
    const raw = JSON.parse(chartConfigJson) as Record<string, unknown>;
    const chartType =
      raw.chartType === 'bar' || raw.chartType === 'line' || raw.chartType === 'pie' || raw.chartType === 'radar'
        ? raw.chartType
        : 'bar';
    const palette =
      raw.palette === 'business' || raw.palette === 'aurora' || raw.palette === 'emerald' || raw.palette === 'sunset'
        ? raw.palette
        : 'business';

    return {
      chartType,
      xField: typeof raw.xField === 'string' ? raw.xField : '',
      yFields: Array.isArray(raw.yFields)
        ? raw.yFields.filter((item): item is string => typeof item === 'string')
        : [],
      palette,
      smooth: typeof raw.smooth === 'boolean' ? raw.smooth : true,
      showLabel: typeof raw.showLabel === 'boolean' ? raw.showLabel : false,
    };
  } catch {
    return defaults;
  }
}

export function formatCellValue(value: unknown, formatter: ColumnFormatter = 'default'): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }

  if (formatter === 'currency') {
    const num = Number(value);
    if (Number.isNaN(num)) return String(value);
    const sign = num < 0 ? '-' : '';
    return `${sign}¥${Math.abs(num).toLocaleString('zh-CN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  }

  if (formatter === 'percent') {
    const num = Number(value);
    if (Number.isNaN(num)) return String(value);
    return `${num.toFixed(2)}%`;
  }

  if (formatter === 'number') {
    const num = Number(value);
    if (Number.isNaN(num)) return String(value);
    return num.toLocaleString('zh-CN', {
      maximumFractionDigits: 2,
    });
  }

  return String(value);
}

export function formatCompactCurrency(value: number): string {
  if (Math.abs(value) >= 100000000) {
    return `¥${(value / 100000000).toFixed(2)}亿`;
  }
  if (Math.abs(value) >= 10000) {
    return `¥${(value / 10000).toFixed(2)}万`;
  }
  return `¥${value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`;
}

export function getTagType(value: unknown): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  const str = String(value ?? '').trim();
  if (['已完成', '华东', '直客', '企业数据中台'].includes(str)) return 'success';
  if (['回款中', '华南', '渠道代理', '智慧供应链SaaS'].includes(str)) return 'warning';
  if (['实施中', '华北', '云市场', '低代码报表引擎'].includes(str)) return 'primary';
  if (['异常', '已取消', '失败'].includes(str)) return 'danger';
  return 'info';
}

export function exportRowsToCsv(
  filename: string,
  columns: ColumnConfig[],
  rows: Record<string, unknown>[]
): void {
  const escapeCell = (val: unknown) => {
    const str = val === null || val === undefined ? '' : String(val);
    if (/[",\r\n]/.test(str)) {
      return `"${str.replace(/"/g, '""')}"`;
    }
    return str;
  };

  const headerLine = columns.map((c) => escapeCell(c.label || c.key)).join(',');
  const bodyLines = rows.map((row) =>
    columns.map((c) => escapeCell(row[c.key])).join(',')
  );

  const csvContent = '\uFEFF' + [headerLine, ...bodyLines].join('\r\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename.endsWith('.csv') ? filename : `${filename}.csv`;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}
