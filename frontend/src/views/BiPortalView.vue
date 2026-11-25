<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import {
  DataAnalysis,
  Delete,
  Download,
  EditPen,
  Money,
  Plus,
  Refresh,
  Search,
  ShoppingCart,
  TrendCharts,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import EChartCard from '../components/EChartCard.vue';
import DataTablePreview from '../components/DataTablePreview.vue';
import { useDataStationStore } from '../stores/datastation';
import type { ReportConfig } from '../types/datastation';
import {
  exportRowsToCsv,
  formatCompactCurrency,
  parseChartConfig,
  parseColumnsConfig,
} from '../utils/formatters';

type CardViewMode = 'chart' | 'table' | 'split';

const store = useDataStationStore();
const selectedCategory = ref<string>('全部');
const keyword = ref<string>('');
const viewModes = reactive<Record<number, CardViewMode>>({});

const categories = computed(() => {
  const set = new Set<string>(['全部']);
  for (const r of store.reports) {
    if (r.category) set.add(r.category);
  }
  return Array.from(set);
});

const filteredReports = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return store.reports.filter((report) => {
    const matchCategory =
      selectedCategory.value === '全部' || report.category === selectedCategory.value;
    const matchKw =
      !kw ||
      report.name.toLowerCase().includes(kw) ||
      (report.description || '').toLowerCase().includes(kw) ||
      report.category.toLowerCase().includes(kw);
    return matchCategory && matchKw;
  });
});

const grossMarginRate = computed(() => {
  const kpis = store.portalKpis;
  if (!kpis || kpis.totalRevenue <= 0) return '0.0%';
  return `${((kpis.totalProfit * 100) / kpis.totalRevenue).toFixed(1)}%`;
});

function getViewMode(id: number): CardViewMode {
  return viewModes[id] || 'chart';
}

function setViewMode(id: number, mode: CardViewMode) {
  viewModes[id] = mode;
}

function getResolvedColumns(report: ReportConfig) {
  const runData = store.reportResults[report.id];
  return parseColumnsConfig(report.columnsJson, runData?.columns || []);
}

function getResolvedChart(report: ReportConfig) {
  return parseChartConfig(report.chartConfigJson);
}

function formatUpdatedTime(iso: string): string {
  if (!iso) return '-';
  return iso.replace('T', ' ').slice(0, 16);
}

function handleExportCsv(report: ReportConfig) {
  const runData = store.reportResults[report.id];
  if (!runData || !runData.rows.length) {
    ElMessage.warning('暂无可导出的数据行');
    return;
  }
  const cols = getResolvedColumns(report);
  exportRowsToCsv(report.name, cols, runData.rows);
  ElMessage.success(`已导出「${report.name}.csv」（共 ${runData.rows.length} 行）`);
}

async function handleDelete(report: ReportConfig) {
  try {
    await store.removeReport(report.id);
    ElMessage.success(`已删除报表「${report.name}」`);
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除报表失败');
  }
}

async function handleRefreshAll() {
  try {
    await store.refreshPortal();
    ElMessage.success('驾驶舱数据已全面刷新');
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '刷新失败');
  }
}
</script>

<template>
  <div class="portal-page">
    <!-- Top KPI Summary Cards -->
    <section class="kpi-grid">
      <div class="kpi-card kpi-blue">
        <div class="kpi-head">
          <span class="kpi-title">累计签约总营收</span>
          <span class="kpi-icon"><el-icon><Money /></el-icon></span>
        </div>
        <div class="kpi-value">
          {{ store.portalKpis ? formatCompactCurrency(store.portalKpis.totalRevenue) : '--' }}
        </div>
        <div class="kpi-foot">
          <span>覆盖华东 / 华北 / 华南 / 西南四大区域</span>
        </div>
      </div>

      <div class="kpi-card kpi-emerald">
        <div class="kpi-head">
          <span class="kpi-title">累计经营毛利</span>
          <span class="kpi-icon"><el-icon><TrendCharts /></el-icon></span>
        </div>
        <div class="kpi-value">
          {{ store.portalKpis ? formatCompactCurrency(store.portalKpis.totalProfit) : '--' }}
        </div>
        <div class="kpi-foot">
          <span>综合毛利率 <b>{{ grossMarginRate }}</b></span>
        </div>
      </div>

      <div class="kpi-card kpi-indigo">
        <div class="kpi-head">
          <span class="kpi-title">销售订单总量</span>
          <span class="kpi-icon"><el-icon><ShoppingCart /></el-icon></span>
        </div>
        <div class="kpi-value">
          {{ store.portalKpis ? `${store.portalKpis.totalOrders} 单` : '--' }}
        </div>
        <div class="kpi-foot">
          <span>来源表 <code>biz_sales_orders</code></span>
        </div>
      </div>

      <div class="kpi-card kpi-amber">
        <div class="kpi-head">
          <span class="kpi-title">平均渠道 ROI</span>
          <span class="kpi-icon"><el-icon><DataAnalysis /></el-icon></span>
        </div>
        <div class="kpi-value">
          {{ store.portalKpis ? `${store.portalKpis.avgRoi.toFixed(2)}x` : '--' }}
        </div>
        <div class="kpi-foot">
          <span>已发布 BI 报表 <b>{{ store.reports.length }}</b> 张</span>
        </div>
      </div>
    </section>

    <!-- Toolbar -->
    <section class="portal-toolbar">
      <div class="toolbar-left">
        <el-radio-group v-model="selectedCategory" size="default">
          <el-radio-button v-for="cat in categories" :key="cat" :value="cat">
            {{ cat }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div class="toolbar-right">
        <el-input
          v-model="keyword"
          placeholder="搜索报表名称或业务描述..."
          clearable
          style="width: 240px"
          :prefix-icon="Search"
        />
        <el-button
          :icon="Refresh"
          :loading="store.portalBusy"
          @click="handleRefreshAll"
        >
          全局刷新
        </el-button>
        <el-button
          type="primary"
          :icon="Plus"
          @click="store.openInStudio()"
        >
          新建报表
        </el-button>
      </div>
    </section>

    <!-- Report Cards Grid -->
    <section v-if="filteredReports.length > 0" class="report-grid">
      <article
        v-for="report in filteredReports"
        :key="report.id"
        class="report-card"
      >
        <header class="card-header">
          <div class="card-title-block">
            <div class="card-title-row">
              <h3 class="card-title">{{ report.name }}</h3>
              <el-tag size="small" effect="plain" type="primary">{{ report.category }}</el-tag>
              <span
                v-if="store.reportResults[report.id]"
                class="exec-pill"
              >
                {{ store.reportResults[report.id].executionMs }} ms ·
                {{ store.reportResults[report.id].rowCount }} 行
              </span>
            </div>
            <p class="card-desc">
              {{ report.description || '基于只读 SQL 实时聚合渲染的 BI 分析卡片' }}
            </p>
          </div>

          <div class="card-actions">
            <el-button-group size="small">
              <el-button
                :type="getViewMode(report.id) === 'chart' ? 'primary' : 'default'"
                @click="setViewMode(report.id, 'chart')"
              >
                图表
              </el-button>
              <el-button
                :type="getViewMode(report.id) === 'table' ? 'primary' : 'default'"
                @click="setViewMode(report.id, 'table')"
              >
                明细
              </el-button>
              <el-button
                :type="getViewMode(report.id) === 'split' ? 'primary' : 'default'"
                @click="setViewMode(report.id, 'split')"
              >
                双栏
              </el-button>
            </el-button-group>

            <el-tooltip content="导出 CSV（含 UTF-8 BOM）" placement="top">
              <el-button
                size="small"
                :icon="Download"
                circle
                @click="handleExportCsv(report)"
              />
            </el-tooltip>

            <el-tooltip content="刷新当前报表" placement="top">
              <el-button
                size="small"
                :icon="Refresh"
                circle
                :loading="Boolean(store.reportLoading[report.id])"
                @click="store.runSingleReport(report.id)"
              />
            </el-tooltip>

            <el-tooltip content="在低代码设计器中编辑" placement="top">
              <el-button
                size="small"
                :icon="EditPen"
                circle
                @click="store.openInStudio(report)"
              />
            </el-tooltip>

            <el-popconfirm
              title="确定要删除该 BI 报表吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(report)"
            >
              <template #reference>
                <el-button
                  size="small"
                  :icon="Delete"
                  circle
                  type="danger"
                  plain
                />
              </template>
            </el-popconfirm>
          </div>
        </header>

        <div v-loading="Boolean(store.reportLoading[report.id])" class="card-body">
          <el-alert
            v-if="store.reportErrors[report.id]"
            type="error"
            :title="store.reportErrors[report.id]"
            show-icon
            :closable="false"
          />

          <template v-else-if="store.reportResults[report.id]">
            <div v-if="getViewMode(report.id) === 'chart'">
              <EChartCard
                :chart-config="getResolvedChart(report)"
                :columns="getResolvedColumns(report)"
                :rows="store.reportResults[report.id].rows"
                :height="300"
              />
            </div>

            <div v-else-if="getViewMode(report.id) === 'table'">
              <DataTablePreview
                :columns="getResolvedColumns(report)"
                :rows="store.reportResults[report.id].rows"
                :max-height="300"
              />
            </div>

            <div v-else class="split-body">
              <EChartCard
                :chart-config="getResolvedChart(report)"
                :columns="getResolvedColumns(report)"
                :rows="store.reportResults[report.id].rows"
                :height="240"
              />
              <DataTablePreview
                :columns="getResolvedColumns(report)"
                :rows="store.reportResults[report.id].rows"
                :max-height="210"
              />
            </div>
          </template>
        </div>

        <footer class="card-footer">
          <span>报表编号 #{{ report.id }}</span>
          <span>最近更新：{{ formatUpdatedTime(report.updatedAt) }}</span>
        </footer>
      </article>
    </section>

    <el-empty
      v-else
      description="当前筛选条件下暂无报表，点击右上角「新建报表」进入低代码设计器创建"
      style="padding: 64px 0"
    >
      <el-button type="primary" @click="store.openInStudio()">新建低代码报表</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.portal-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.kpi-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-title {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.kpi-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
}

.kpi-blue .kpi-icon {
  background: #eff6ff;
  color: #2563eb;
}

.kpi-emerald .kpi-icon {
  background: #ecfdf5;
  color: #059669;
}

.kpi-indigo .kpi-icon {
  background: #eef2ff;
  color: #4f46e5;
}

.kpi-amber .kpi-icon {
  background: #fffbeb;
  color: #d97706;
}

.kpi-value {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
}

.kpi-foot {
  font-size: 12px;
  color: #64748b;
}

.kpi-foot code {
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 4px;
  color: #334155;
  font-size: 11px;
}

.portal-toolbar {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.report-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.report-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-title-block {
  flex: 1;
  min-width: 220px;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.exec-pill {
  font-size: 11px;
  color: #475569;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
}

.card-desc {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: #64748b;
  line-height: 1.45;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.card-body {
  min-height: 220px;
}

.split-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card-footer {
  border-top: 1px solid #f1f5f9;
  padding-top: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: #94a3b8;
}

@media (max-width: 1180px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .report-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
