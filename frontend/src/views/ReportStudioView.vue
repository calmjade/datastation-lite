<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  CaretRight,
  Check,
  CopyDocument,
  Delete,
  DocumentAdd,
  Grid,
  Lock,
  MagicStick,
  Setting,
  Timer,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import EChartCard from '../components/EChartCard.vue';
import DataTablePreview from '../components/DataTablePreview.vue';
import { useDataStationStore } from '../stores/datastation';
import type { ChartType, PaletteKey } from '../types/datastation';
import { FORMATTER_OPTIONS, PALETTES } from '../utils/formatters';

interface SqlTemplate {
  title: string;
  category: string;
  description: string;
  chartType: ChartType;
  sql: string;
}

const SQL_TEMPLATES: SqlTemplate[] = [
  {
    title: '区域营收与毛利排行',
    category: '经营分析',
    description: '按销售大区聚合订单量、总营收、毛利及综合毛利率',
    chartType: 'bar',
    sql: `SELECT region,
       COUNT(*) AS order_count,
       ROUND(SUM(amount), 2) AS total_amount,
       ROUND(SUM(profit), 2) AS total_profit,
       ROUND(SUM(profit) * 100.0 / SUM(amount), 2) AS margin_rate
FROM biz_sales_orders
GROUP BY region
ORDER BY total_amount DESC`,
  },
  {
    title: '月度营收增长趋势',
    category: '财务看板',
    description: '按月统计 2026 年订单规模、合同总额及毛利变化趋势',
    chartType: 'line',
    sql: `SELECT SUBSTRING(CAST(order_date AS VARCHAR), 1, 7) AS order_month,
       COUNT(*) AS order_count,
       ROUND(SUM(amount), 2) AS total_amount,
       ROUND(SUM(profit), 2) AS total_profit
FROM biz_sales_orders
GROUP BY SUBSTRING(CAST(order_date AS VARCHAR), 1, 7)
ORDER BY order_month ASC`,
  },
  {
    title: '产品线结构占比',
    category: '产品运营',
    description: '按产品线对比营收贡献占比与盈利表现',
    chartType: 'pie',
    sql: `SELECT product_line,
       COUNT(*) AS order_count,
       ROUND(SUM(amount), 2) AS total_amount,
       ROUND(SUM(profit), 2) AS total_profit
FROM biz_sales_orders
GROUP BY product_line
ORDER BY total_amount DESC`,
  },
  {
    title: '渠道 ROI 多维雷达',
    category: '营销获客',
    description: '综合评估各获客渠道的线索、签约数与平均 ROI',
    chartType: 'radar',
    sql: `SELECT channel,
       SUM(leads) AS total_leads,
       SUM(signed_deals) AS total_deals,
       ROUND(AVG(conversion_rate), 2) AS avg_conv_rate,
       ROUND(AVG(roi), 2) AS avg_roi
FROM biz_channel_kpi
GROUP BY channel
ORDER BY avg_roi DESC`,
  },
  {
    title: '高金额签约订单 Top 15',
    category: '经营分析',
    description: '筛选合同金额最高的 15 笔重点客户订单明细',
    chartType: 'bar',
    sql: `SELECT order_no,
       customer_name,
       region,
       product_line,
       amount,
       profit,
       status
FROM biz_sales_orders
ORDER BY amount DESC
LIMIT 15`,
  },
];

const CATEGORY_OPTIONS = ['经营分析', '财务看板', '产品运营', '营销获客', '自定义分析'];
const CHART_TYPE_OPTIONS: { value: ChartType; label: string }[] = [
  { value: 'bar', label: '柱状图' },
  { value: 'line', label: '折线图' },
  { value: 'pie', label: '饼图' },
  { value: 'radar', label: '雷达图' },
];

const store = useDataStationStore();
const previewTab = ref<'chart' | 'table' | 'raw'>('chart');
const sqlTextareaRef = ref<HTMLTextAreaElement | null>(null);
const expandedTables = ref<string[]>(['biz_sales_orders', 'biz_channel_kpi']);

const draft = computed(() => store.studioDraft);

const availableFields = computed(() =>
  draft.value.columns.map((c) => ({
    value: c.key,
    label: c.label && c.label !== c.key ? `${c.label} (${c.key})` : c.key,
  }))
);

function insertSnippet(snippet: string) {
  const el = sqlTextareaRef.value;
  if (!el) {
    draft.value.sqlText = `${draft.value.sqlText}\n${snippet}`.trim();
    return;
  }
  const start = el.selectionStart ?? draft.value.sqlText.length;
  const end = el.selectionEnd ?? start;
  const current = draft.value.sqlText;
  draft.value.sqlText = current.slice(0, start) + snippet + current.slice(end);
  requestAnimationFrame(() => {
    el.focus();
    const pos = start + snippet.length;
    el.setSelectionRange(pos, pos);
  });
}

async function applyTemplate(tpl: SqlTemplate) {
  draft.value.name = tpl.title;
  draft.value.category = tpl.category;
  draft.value.description = tpl.description;
  draft.value.sqlText = tpl.sql;
  draft.value.chartConfig.chartType = tpl.chartType;
  draft.value.chartConfig.xField = '';
  draft.value.chartConfig.yFields = [];
  await handleRunQuery();
}

async function handleRunQuery() {
  await store.runStudioQuery();
  if (!draft.value.queryError && draft.value.queryResult) {
    ElMessage.success(
      `查询完成：返回 ${draft.value.queryResult.rowCount} 行（耗时 ${draft.value.queryResult.executionMs} ms）`
    );
  }
}

function handleSqlKeydown(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault();
    handleRunQuery();
  }
}

async function handleSaveReport(asNew = false) {
  if (!draft.value.name.trim()) {
    ElMessage.warning('请先填写报表名称');
    return;
  }
  if (!draft.value.sqlText.trim()) {
    ElMessage.warning('SQL 查询语句不能为空');
    return;
  }

  try {
    const saved = await store.saveStudioReport(asNew);
    ElMessage.success(
      asNew || !draft.value.editingId
        ? `报表「${saved.name}」已发布至驾驶舱`
        : `报表「${saved.name}」配置已更新`
    );
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败');
  }
}

function handleResetNew() {
  store.openInStudio();
}
</script>

<template>
  <div class="studio-layout">
    <!-- Left Sidebar: Schema Explorer & Preset Templates -->
    <aside class="studio-sidebar panel-card">
      <div class="panel-header">
        <span class="panel-title">
          <el-icon><Grid /></el-icon>
          数据表与字段树
        </span>
        <span class="panel-hint">点击插入 SQL</span>
      </div>

      <div class="schema-tree">
        <el-collapse v-model="expandedTables">
          <el-collapse-item
            v-for="table in store.tables"
            :key="table.name"
            :name="table.name"
          >
            <template #title>
              <div class="tree-table-title">
                <span class="mono-name">{{ table.name }}</span>
                <el-tag size="small" type="info" effect="plain">{{ table.rowCount }} 行</el-tag>
              </div>
            </template>

            <div class="tree-table-actions">
              <span class="table-cn-label">{{ table.label }}</span>
              <el-button
                link
                type="primary"
                size="small"
                @click="insertSnippet(table.name)"
              >
                插入表名
              </el-button>
            </div>

            <ul class="column-list">
              <li
                v-for="col in table.columns"
                :key="col.name"
                class="column-item"
                @click="insertSnippet(col.name)"
              >
                <span class="mono-col">{{ col.name }}</span>
                <span class="col-type-badge">{{ col.type }}</span>
              </li>
            </ul>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div class="template-section">
        <div class="panel-header sub-header">
          <span class="panel-title">
            <el-icon><MagicStick /></el-icon>
            预设分析模板
          </span>
        </div>
        <div class="template-list">
          <button
            v-for="tpl in SQL_TEMPLATES"
            :key="tpl.title"
            type="button"
            class="template-btn"
            @click="applyTemplate(tpl)"
          >
            <span class="tpl-name">{{ tpl.title }}</span>
            <el-tag size="small" effect="light">{{ tpl.chartType }}</el-tag>
          </button>
        </div>
      </div>
    </aside>

    <!-- Center Workspace: Report Meta + SQL Editor + Live Preview -->
    <main class="studio-center">
      <!-- Report Metadata Bar -->
      <section class="panel-card meta-bar">
        <div class="meta-bar-top">
          <div class="meta-status">
            <el-tag :type="draft.editingId ? 'warning' : 'success'" effect="dark" size="small">
              {{ draft.editingId ? `编辑模式 #${draft.editingId}` : '新建报表模式' }}
            </el-tag>
            <span class="meta-tip">配置完成后点击右侧「保存发布报表」即可同步至 BI 驾驶舱</span>
          </div>
          <el-button size="small" :icon="DocumentAdd" @click="handleResetNew">
            新建空白报表
          </el-button>
        </div>

        <div class="meta-inputs">
          <el-input
            v-model="draft.name"
            placeholder="请输入报表名称（如：华东区产品毛利分析）"
            style="flex: 1.4"
          >
            <template #prepend>报表名称</template>
          </el-input>

          <el-select
            v-model="draft.category"
            filterable
            allow-create
            placeholder="选择分类"
            style="width: 170px"
          >
            <el-option v-for="cat in CATEGORY_OPTIONS" :key="cat" :label="cat" :value="cat" />
          </el-select>

          <el-input
            v-model="draft.description"
            placeholder="业务口径说明（可选）"
            style="flex: 1.6"
          >
            <template #prepend>说明</template>
          </el-input>
        </div>
      </section>

      <!-- SQL Editor Card -->
      <section class="panel-card sql-card">
        <div class="sql-toolbar">
          <div class="sql-toolbar-left">
            <span class="panel-title">SQL 查询编辑器</span>
            <el-tag size="small" type="success" effect="plain">
              <el-icon style="vertical-align: -2px; margin-right: 3px"><Lock /></el-icon>
              只读防护 · 仅允许 SELECT / WITH · 最多 500 行
            </el-tag>
            <span v-if="draft.queryResult" class="exec-stat">
              <el-icon><Timer /></el-icon>
              {{ draft.queryResult.executionMs }} ms · {{ draft.queryResult.rowCount }} 行
            </span>
          </div>

          <div class="sql-toolbar-right">
            <el-button
              size="small"
              :icon="Delete"
              @click="draft.sqlText = ''"
            >
              清空
            </el-button>
            <el-button
              type="primary"
              :icon="CaretRight"
              :loading="draft.runningQuery"
              @click="handleRunQuery"
            >
              运行查询 (Ctrl+Enter)
            </el-button>
          </div>
        </div>

        <textarea
          ref="sqlTextareaRef"
          v-model="draft.sqlText"
          class="sql-textarea"
          rows="7"
          spellcheck="false"
          placeholder="输入只读 SELECT 或 WITH 查询语句，按 Ctrl+Enter 快速运行..."
          @keydown="handleSqlKeydown"
        />

        <el-alert
          v-if="draft.queryError"
          type="error"
          :title="draft.queryError"
          show-icon
          :closable="false"
          style="margin-top: 10px"
        />
      </section>

      <!-- Live Preview Card -->
      <section class="panel-card preview-card">
        <div class="preview-header">
          <span class="panel-title">实时联动预览</span>
          <el-radio-group v-model="previewTab" size="small">
            <el-radio-button value="chart">可视化图表</el-radio-button>
            <el-radio-button value="table">格式化表格</el-radio-button>
            <el-radio-button value="raw">原始查询结果</el-radio-button>
          </el-radio-group>
        </div>

        <div v-loading="draft.runningQuery" class="preview-body">
          <template v-if="draft.queryResult">
            <EChartCard
              v-if="previewTab === 'chart'"
              :chart-config="draft.chartConfig"
              :columns="draft.columns"
              :rows="draft.queryResult.rows"
              :height="320"
            />

            <DataTablePreview
              v-else-if="previewTab === 'table'"
              :columns="draft.columns"
              :rows="draft.queryResult.rows"
              :max-height="320"
            />

            <el-table
              v-else
              :data="draft.queryResult.rows"
              max-height="320"
              size="small"
              border
              stripe
            >
              <el-table-column
                v-for="col in draft.queryResult.columns"
                :key="col.name"
                :prop="col.name"
                :label="`${col.name} (${col.type})`"
                min-width="130"
                show-overflow-tooltip
              />
            </el-table>
          </template>
          <el-empty v-else description="点击「运行查询」即可预览图表与数据" :image-size="72" />
        </div>
      </section>
    </main>

    <!-- Right Panel: Visual Configurator -->
    <aside class="studio-config panel-card">
      <div class="panel-header">
        <span class="panel-title">
          <el-icon><Setting /></el-icon>
          可视化与字段配置
        </span>
      </div>

      <div class="config-scroll">
        <!-- Chart Settings -->
        <div class="config-group">
          <div class="config-group-title">图表配置</div>

          <div class="field-item">
            <label>图表类型</label>
            <el-radio-group v-model="draft.chartConfig.chartType" size="small" style="width: 100%">
              <el-radio-button
                v-for="opt in CHART_TYPE_OPTIONS"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="field-item">
            <label>X 轴 / 分类维度</label>
            <el-select
              v-model="draft.chartConfig.xField"
              placeholder="选择维度字段"
              style="width: 100%"
            >
              <el-option
                v-for="f in availableFields"
                :key="f.value"
                :label="f.label"
                :value="f.value"
              />
            </el-select>
          </div>

          <div class="field-item">
            <label>
              Y 轴 / 指标列
              <span class="sub-hint">{{ draft.chartConfig.chartType === 'pie' ? '（饼图取首个指标）' : '（可多选）' }}</span>
            </label>
            <el-select
              v-model="draft.chartConfig.yFields"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="选择一个或多个数值指标"
              style="width: 100%"
            >
              <el-option
                v-for="f in availableFields"
                :key="f.value"
                :label="f.label"
                :value="f.value"
              />
            </el-select>
          </div>

          <div class="field-item">
            <label>企业级配色主题</label>
            <div class="palette-grid">
              <button
                v-for="(p, key) in PALETTES"
                :key="key"
                type="button"
                class="palette-item"
                :class="{ active: draft.chartConfig.palette === key }"
                @click="draft.chartConfig.palette = key as PaletteKey"
              >
                <span class="palette-dots">
                  <i
                    v-for="c in p.colors.slice(0, 4)"
                    :key="c"
                    :style="{ backgroundColor: c }"
                  />
                </span>
                <span class="palette-label">{{ p.label }}</span>
              </button>
            </div>
          </div>

          <div class="switch-row">
            <div v-if="draft.chartConfig.chartType === 'line'" class="switch-item">
              <span>平滑曲线</span>
              <el-switch v-model="draft.chartConfig.smooth" size="small" />
            </div>
            <div class="switch-item">
              <span>显示数据标签</span>
              <el-switch v-model="draft.chartConfig.showLabel" size="small" />
            </div>
          </div>
        </div>

        <!-- Column Formatter Settings -->
        <div class="config-group">
          <div class="config-group-title">
            字段别名与格式化
            <span class="sub-hint">({{ draft.columns.length }})</span>
          </div>

          <div v-if="draft.columns.length > 0" class="column-config-list">
            <div v-for="col in draft.columns" :key="col.key" class="column-config-row">
              <div class="col-key-line">
                <code>{{ col.key }}</code>
              </div>
              <div class="col-edit-line">
                <el-input
                  v-model="col.label"
                  size="small"
                  placeholder="中文列标题"
                  style="flex: 1"
                />
                <el-select v-model="col.formatter" size="small" style="width: 108px">
                  <el-option
                    v-for="fmt in FORMATTER_OPTIONS"
                    :key="fmt.value"
                    :label="fmt.label"
                    :value="fmt.value"
                  />
                </el-select>
              </div>
            </div>
          </div>
          <el-empty v-else description="运行查询后自动识别字段" :image-size="48" />
        </div>
      </div>

      <div class="config-footer">
        <el-button
          type="primary"
          :icon="Check"
          :loading="draft.saving"
          style="width: 100%"
          @click="handleSaveReport(false)"
        >
          {{ draft.editingId ? '保存并更新报表' : '保存发布报表' }}
        </el-button>
        <el-button
          v-if="draft.editingId"
          :icon="CopyDocument"
          :loading="draft.saving"
          style="width: 100%; margin-left: 0"
          @click="handleSaveReport(true)"
        >
          另存为新报表
        </el-button>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.studio-layout {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr) 310px;
  gap: 16px;
  align-items: start;
}

.panel-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.panel-hint {
  font-size: 11px;
  color: #94a3b8;
}

.studio-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(100vh - 130px);
  overflow-y: auto;
}

.tree-table-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 6px;
}

.mono-name {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
  font-weight: 600;
  color: #1e293b;
}

.tree-table-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.table-cn-label {
  font-size: 12px;
  color: #64748b;
}

.column-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.column-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.column-item:hover {
  background: #eff6ff;
}

.mono-col {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  color: #334155;
}

.col-type-badge {
  font-size: 10px;
  color: #64748b;
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 4px;
}

.sub-header {
  margin-top: 4px;
  margin-bottom: 8px;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.template-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}

.template-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.tpl-name {
  font-size: 12.5px;
  color: #1e293b;
  font-weight: 500;
}

.studio-center {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.meta-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.meta-bar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.meta-status {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-tip {
  font-size: 12px;
  color: #64748b;
}

.meta-inputs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.sql-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.sql-toolbar-left,
.sql-toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.exec-stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #059669;
  background: #ecfdf5;
  padding: 2px 8px;
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
}

.sql-textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 12px 14px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.55;
  color: #0f172a;
  background: #f8fafc;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.sql-textarea:focus {
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.preview-body {
  min-height: 260px;
}

.studio-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(100vh - 130px);
}

.config-scroll {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-right: 2px;
}

.config-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.config-group-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.sub-hint {
  font-weight: 400;
  color: #94a3b8;
  font-size: 11px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-item label {
  font-size: 12px;
  color: #475569;
  font-weight: 500;
}

.palette-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.15s ease;
}

.palette-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.palette-dots {
  display: inline-flex;
  gap: 2px;
}

.palette-dots i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.palette-label {
  font-size: 11.5px;
  color: #334155;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.switch-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
}

.column-config-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.column-config-row {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.col-key-line code {
  font-size: 11.5px;
  color: #2563eb;
  font-weight: 600;
}

.col-edit-line {
  display: flex;
  gap: 6px;
  align-items: center;
}

.config-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 6px;
}

@media (max-width: 1280px) {
  .studio-layout {
    grid-template-columns: 240px minmax(0, 1fr);
  }
  .studio-config {
    grid-column: 1 / -1;
    max-height: none;
  }
}

@media (max-width: 900px) {
  .studio-layout {
    grid-template-columns: 1fr;
  }
  .studio-sidebar {
    max-height: none;
  }
}
</style>
