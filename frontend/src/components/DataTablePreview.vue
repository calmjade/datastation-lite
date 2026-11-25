<script setup lang="ts">
import type { ColumnConfig } from '../types/datastation';
import { formatCellValue, getTagType } from '../utils/formatters';

withDefaults(
  defineProps<{
    columns: ColumnConfig[];
    rows: Record<string, unknown>[];
    maxHeight?: number;
    stripe?: boolean;
  }>(),
  {
    maxHeight: 300,
    stripe: true,
  }
);

function isRightAligned(formatter: ColumnConfig['formatter']): boolean {
  return formatter === 'currency' || formatter === 'percent' || formatter === 'number';
}
</script>

<template>
  <el-table
    :data="rows"
    :max-height="maxHeight"
    :stripe="stripe"
    size="small"
    border
    style="width: 100%"
    empty-text="暂无匹配数据"
  >
    <el-table-column
      v-for="col in columns"
      :key="col.key"
      :prop="col.key"
      :label="col.label || col.key"
      :align="isRightAligned(col.formatter) ? 'right' : 'left'"
      min-width="110"
      show-overflow-tooltip
    >
      <template #default="{ row }">
        <el-tag
          v-if="col.formatter === 'tag'"
          :type="getTagType(row[col.key])"
          size="small"
          effect="light"
          round
        >
          {{ formatCellValue(row[col.key], 'tag') }}
        </el-tag>
        <span
          v-else-if="col.formatter === 'currency'"
          class="cell-num cell-currency"
        >
          {{ formatCellValue(row[col.key], 'currency') }}
        </span>
        <span
          v-else-if="col.formatter === 'percent'"
          class="cell-num cell-percent"
        >
          {{ formatCellValue(row[col.key], 'percent') }}
        </span>
        <span
          v-else-if="col.formatter === 'number'"
          class="cell-num"
        >
          {{ formatCellValue(row[col.key], 'number') }}
        </span>
        <span v-else>
          {{ formatCellValue(row[col.key], 'default') }}
        </span>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.cell-num {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.cell-currency {
  color: #0f172a;
}

.cell-percent {
  color: #2563eb;
}
</style>
