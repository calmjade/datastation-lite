<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts';
import type { ChartConfig, ColumnConfig } from '../types/datastation';
import { buildEChartsOption } from '../utils/chartBuilder';

const props = withDefaults(
  defineProps<{
    chartConfig: ChartConfig;
    columns: ColumnConfig[];
    rows: Record<string, unknown>[];
    height?: number;
  }>(),
  {
    height: 300,
  }
);

const containerRef = ref<HTMLDivElement | null>(null);
let chartInstance: echarts.ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;

const option = computed(() =>
  buildEChartsOption(props.chartConfig, props.columns, props.rows)
);

function renderChart() {
  if (!containerRef.value) return;
  if (!option.value) {
    chartInstance?.clear();
    return;
  }
  if (!chartInstance) {
    chartInstance = echarts.init(containerRef.value);
  }
  chartInstance.setOption(option.value, true);
}

watch(
  option,
  () => {
    renderChart();
  },
  { deep: true }
);

onMounted(() => {
  renderChart();
  if (containerRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize();
    });
    resizeObserver.observe(containerRef.value);
  }
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  resizeObserver = null;
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<template>
  <div class="echart-wrapper" :style="{ height: `${height}px` }">
    <div v-show="Boolean(option)" ref="containerRef" class="echart-canvas" />
    <div v-if="!option" class="echart-empty">
      <el-empty
        :image-size="64"
        description="请选择有效的 X 轴维度与至少一个 Y 轴指标以渲染图表"
      />
    </div>
  </div>
</template>

<style scoped>
.echart-wrapper {
  position: relative;
  width: 100%;
}

.echart-canvas {
  width: 100%;
  height: 100%;
}

.echart-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
