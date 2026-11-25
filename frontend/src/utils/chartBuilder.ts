import type { EChartsOption } from 'echarts';
import type { ChartConfig, ColumnConfig } from '../types/datastation';
import { PALETTES } from './formatters';

export function buildEChartsOption(
  chartConfig: ChartConfig,
  columns: ColumnConfig[],
  rows: Record<string, unknown>[]
): EChartsOption | null {
  if (!rows.length || !chartConfig.xField || !chartConfig.yFields.length) {
    return null;
  }

  const labelMap = new Map(columns.map((c) => [c.key, c.label || c.key]));
  const palette = PALETTES[chartConfig.palette]?.colors || PALETTES.business.colors;
  const xField = chartConfig.xField;
  const yFields = chartConfig.yFields;

  if (chartConfig.chartType === 'pie') {
    const metricKey = yFields[0];
    const metricLabel = labelMap.get(metricKey) || metricKey;
    const pieData = rows.map((row) => ({
      name: String(row[xField] ?? '-'),
      value: Number(row[metricKey] ?? 0),
    }));

    return {
      color: palette,
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(15, 23, 42, 0.92)',
        borderWidth: 0,
        textStyle: { color: '#F8FAFC', fontSize: 12 },
        formatter: `{b}<br/>${metricLabel}: <b>{c}</b> ({d}%)`,
      },
      legend: {
        bottom: 0,
        type: 'scroll',
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: '#475569', fontSize: 12 },
      },
      series: [
        {
          name: metricLabel,
          type: 'pie',
          radius: ['42%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#FFFFFF',
            borderWidth: 2,
          },
          label: {
            show: chartConfig.showLabel,
            formatter: '{b}: {d}%',
            color: '#334155',
            fontSize: 12,
          },
          data: pieData,
        },
      ],
    };
  }

  if (chartConfig.chartType === 'radar') {
    const indicators = yFields.map((field) => {
      const values = rows.map((r) => Number(r[field] ?? 0)).filter((n) => !Number.isNaN(n));
      const maxVal = values.length ? Math.max(...values) : 100;
      return {
        name: labelMap.get(field) || field,
        max: maxVal > 0 ? Math.ceil(maxVal * 1.2) : 100,
      };
    });

    const seriesData = rows.slice(0, 8).map((row, idx) => ({
      name: String(row[xField] ?? `维度 ${idx + 1}`),
      value: yFields.map((field) => Number(row[field] ?? 0)),
      areaStyle: { opacity: 0.16 },
      lineStyle: { width: 2 },
    }));

    return {
      color: palette,
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(15, 23, 42, 0.92)',
        borderWidth: 0,
        textStyle: { color: '#F8FAFC', fontSize: 12 },
      },
      legend: {
        bottom: 0,
        type: 'scroll',
        icon: 'roundRect',
        itemWidth: 12,
        itemHeight: 8,
        textStyle: { color: '#475569', fontSize: 12 },
      },
      radar: {
        indicator: indicators,
        radius: '60%',
        center: ['50%', '46%'],
        splitNumber: 4,
        axisName: { color: '#475569', fontSize: 12 },
        splitArea: {
          areaStyle: { color: ['#F8FAFC', '#FFFFFF'] },
        },
      },
      series: [
        {
          type: 'radar',
          data: seriesData,
        },
      ],
    };
  }

  const categories = rows.map((r) => String(r[xField] ?? '-'));
  const isBar = chartConfig.chartType === 'bar';

  return {
    color: palette,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: isBar ? 'shadow' : 'line' },
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#F8FAFC', fontSize: 12 },
    },
    legend: {
      top: 0,
      right: 0,
      type: 'scroll',
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { color: '#475569', fontSize: 12 },
    },
    grid: {
      top: 38,
      right: 16,
      bottom: categories.length > 8 ? 44 : 28,
      left: 14,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: categories,
      boundaryGap: isBar,
      axisLine: { lineStyle: { color: '#CBD5E1' } },
      axisLabel: {
        color: '#475569',
        fontSize: 11,
        rotate: categories.length > 8 ? 25 : 0,
      },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#E2E8F0', type: 'dashed' } },
      axisLabel: {
        color: '#64748B',
        fontSize: 11,
        formatter: (val: number) => {
          if (Math.abs(val) >= 10000) {
            return `${(val / 10000).toFixed(1)}w`;
          }
          return String(val);
        },
      },
    },
    series: yFields.map((field, index) => ({
      name: labelMap.get(field) || field,
      type: isBar ? 'bar' : 'line',
      data: rows.map((r) => Number(r[field] ?? 0)),
      smooth: !isBar && chartConfig.smooth,
      barMaxWidth: 32,
      itemStyle: isBar ? { borderRadius: [5, 5, 0, 0] } : undefined,
      areaStyle: !isBar && index === 0 ? { opacity: 0.14 } : undefined,
      label: {
        show: chartConfig.showLabel,
        position: 'top',
        color: '#334155',
        fontSize: 11,
      },
    })),
  };
}
