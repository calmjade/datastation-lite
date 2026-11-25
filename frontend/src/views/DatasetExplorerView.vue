<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import {
  Coin,
  CopyDocument,
  DataBoard,
  MagicStick,
  Refresh,
  Tickets,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { executeQuery } from '../api/client';
import { useDataStationStore } from '../stores/datastation';
import type { QueryExecuteResponse, TableMeta } from '../types/datastation';

const store = useDataStationStore();
const selectedTableName = ref<string>('biz_sales_orders');
const samplePreview = ref<QueryExecuteResponse | null>(null);
const loadingSample = ref(false);

const selectedTable = computed<TableMeta | undefined>(() => {
  return (
    store.tables.find((t) => t.name === selectedTableName.value) || store.tables[0]
  );
});

const totalRowCount = computed(() =>
  store.tables.reduce((acc, item) => acc + item.rowCount, 0)
);

async function loadTableSample(tableName: string) {
  if (!tableName) return;
  loadingSample.value = true;
  try {
    samplePreview.value = await executeQuery(`SELECT * FROM ${tableName} LIMIT 10`, 10);
  } catch {
    samplePreview.value = null;
  } finally {
    loadingSample.value = false;
  }
}

watch(
  () => selectedTable.value?.name,
  (name) => {
    if (name) {
      loadTableSample(name);
    }
  },
  { immediate: true }
);

function formatSample(val: unknown): string {
  if (val === null || val === undefined) return 'NULL';
  const str = String(val);
  return str.length > 60 ? `${str.slice(0, 60)}...` : str;
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(`已复制：${text}`);
  } catch {
    ElMessage.info(text);
  }
}

function generateGroupByQuery(table: TableMeta, colName: string) {
  const sql = `SELECT ${colName}, COUNT(*) AS record_count\nFROM ${table.name}\nGROUP BY ${colName}\nORDER BY record_count DESC\nLIMIT 50`;
  store.generateSelectToStudio(table, sql);
}
</script>

<template>
  <div class="explorer-page">
    <!-- Top Overview Strip -->
    <section class="overview-strip">
      <div class="ov-item">
        <el-icon class="ov-icon"><Coin /></el-icon>
        <div>
          <div class="ov-label">数据库引擎</div>
          <div class="ov-val">H2 Embedded (MySQL Mode)</div>
        </div>
      </div>
      <div class="ov-item">
        <el-icon class="ov-icon"><Tickets /></el-icon>
        <div>
          <div class="ov-label">已登记业务表</div>
          <div class="ov-val">{{ store.tables.length }} 张</div>
        </div>
      </div>
      <div class="ov-item">
        <el-icon class="ov-icon"><DataBoard /></el-icon>
        <div>
          <div class="ov-label">累计数据记录</div>
          <div class="ov-val">{{ totalRowCount.toLocaleString('zh-CN') }} 条</div>
        </div>
      </div>
      <div class="ov-actions">
        <el-button :icon="Refresh" @click="store.refreshPortal()">同步元数据</el-button>
      </div>
    </section>

    <div class="explorer-body">
      <!-- Table List -->
      <aside class="table-list-panel">
        <div
          v-for="table in store.tables"
          :key="table.name"
          class="table-selector-card"
          :class="{ active: selectedTable?.name === table.name }"
          @click="selectedTableName = table.name"
        >
          <div class="ts-top">
            <span class="ts-name">{{ table.name }}</span>
            <el-tag size="small" effect="plain">{{ table.rowCount }} 行</el-tag>
          </div>
          <div class="ts-label">{{ table.label }}</div>
          <div class="ts-desc">{{ table.description }}</div>
          <div class="ts-meta">字段数：{{ table.columns.length }}</div>
        </div>
      </aside>

      <!-- Table Detail & Sample -->
      <main v-if="selectedTable" class="table-detail-panel">
        <section class="detail-card">
          <div class="detail-header">
            <div>
              <div class="detail-title-row">
                <h2>{{ selectedTable.label }}</h2>
                <code class="detail-table-code">{{ selectedTable.name }}</code>
                <el-tag type="success" size="small">{{ selectedTable.rowCount }} 条记录</el-tag>
              </div>
              <p class="detail-desc">{{ selectedTable.description }}</p>
            </div>

            <div class="detail-actions">
              <el-button
                :icon="CopyDocument"
                @click="copyText(selectedTable.name)"
              >
                复制表名
              </el-button>
              <el-button
                type="primary"
                :icon="MagicStick"
                @click="store.generateSelectToStudio(selectedTable)"
              >
                一键生成 SELECT 并进入设计器
              </el-button>
            </div>
          </div>

          <el-table
            :data="selectedTable.columns"
            border
            stripe
            size="small"
            style="width: 100%"
          >
            <el-table-column prop="name" label="字段名" min-width="160">
              <template #default="{ row }">
                <code class="col-code">{{ row.name }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="数据类型" width="140">
              <template #default="{ row }">
                <el-tag size="small" type="info" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="样例值" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="sample-val">{{ formatSample(row.sample) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="快捷探索" width="160" align="center">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="generateGroupByQuery(selectedTable, row.name)"
                >
                  按此字段聚合统计
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="detail-card">
          <div class="sample-header">
            <h3>实时数据样例（前 10 条）</h3>
            <el-button
              size="small"
              :icon="Refresh"
              :loading="loadingSample"
              @click="loadTableSample(selectedTable.name)"
            >
              刷新样例
            </el-button>
          </div>

          <el-table
            v-loading="loadingSample"
            :data="samplePreview?.rows || []"
            border
            stripe
            size="small"
            max-height="300"
            style="width: 100%"
          >
            <el-table-column
              v-for="col in samplePreview?.columns || []"
              :key="col.name"
              :prop="col.name"
              :label="col.name"
              min-width="130"
              show-overflow-tooltip
            />
          </el-table>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.explorer-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.overview-strip {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.ov-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ov-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 18px;
}

.ov-label {
  font-size: 12px;
  color: #64748b;
}

.ov-val {
  font-size: 14.5px;
  font-weight: 600;
  color: #0f172a;
}

.explorer-body {
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.table-list-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.table-selector-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.table-selector-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.06);
}

.table-selector-card.active {
  border-color: #2563eb;
  background: #f8fbff;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.ts-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ts-name {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.ts-label {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.ts-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.45;
}

.ts-meta {
  margin-top: 8px;
  font-size: 11.5px;
  color: #94a3b8;
}

.table-detail-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-title-row h2 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.detail-table-code {
  background: #f1f5f9;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12.5px;
  font-weight: 600;
}

.detail-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.col-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
  color: #0f172a;
  font-weight: 600;
}

.sample-val {
  color: #475569;
  font-size: 12px;
}

.sample-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sample-header h3 {
  margin: 0;
  font-size: 15px;
  color: #0f172a;
}

@media (max-width: 960px) {
  .explorer-body {
    grid-template-columns: 1fr;
  }
}
</style>
