<script setup lang="ts">
import { onMounted } from 'vue';
import { Coin, DataBoard, EditPen } from '@element-plus/icons-vue';
import { useDataStationStore } from './stores/datastation';
import BiPortalView from './views/BiPortalView.vue';
import ReportStudioView from './views/ReportStudioView.vue';
import DatasetExplorerView from './views/DatasetExplorerView.vue';
import type { ActiveTab } from './types/datastation';

const store = useDataStationStore();

const navItems: { key: ActiveTab; label: string; desc: string }[] = [
  { key: 'portal', label: '驾驶舱门户', desc: 'BI Portal' },
  { key: 'studio', label: '低代码报表设计器', desc: 'Report Studio' },
  { key: 'explorer', label: '数据源与表字典', desc: 'Dataset Explorer' },
];

onMounted(() => {
  store.initApp();
});
</script>

<template>
  <div class="app-shell">
    <header class="top-navbar">
      <div class="nav-inner">
        <div class="brand-block">
          <div class="brand-logo">DS</div>
          <div>
            <div class="brand-title">
              数据驿站 Lite
              <span class="brand-badge">BI Platform</span>
            </div>
            <div class="brand-subtitle">配置化低代码报表与经营决策看板</div>
          </div>
        </div>

        <nav class="nav-tabs" aria-label="主导航">
          <button
            v-for="item in navItems"
            :key="item.key"
            type="button"
            class="nav-tab-btn"
            :class="{ active: store.activeTab === item.key }"
            @click="store.activeTab = item.key"
          >
            <el-icon v-if="item.key === 'portal'"><DataBoard /></el-icon>
            <el-icon v-else-if="item.key === 'studio'"><EditPen /></el-icon>
            <el-icon v-else><Coin /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <div class="nav-status">
          <span
            class="status-dot"
            :class="store.health?.status === 'UP' ? 'online' : 'offline'"
          />
          <span class="status-text">
            {{
              store.health?.status === 'UP'
                ? `引擎在线 · ${store.health.reportCount} 张报表`
                : '连接后端服务中...'
            }}
          </span>
        </div>
      </div>
    </header>

    <main class="main-container">
      <BiPortalView v-if="store.activeTab === 'portal'" />
      <ReportStudioView v-else-if="store.activeTab === 'studio'" />
      <DatasetExplorerView v-else />
    </main>

    <footer class="app-footer">
      <span>数据驿站 Lite (DataStation Lite) · 仅限只读 SQL 查询防线保护</span>
      <span>Author: 曹杰 (calmjade) · MIT License</span>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.top-navbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e2e8f0;
}

.nav-inner {
  max-width: 1480px;
  margin: 0 auto;
  padding: 10px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
  color: #ffffff;
  font-weight: 800;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.04em;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-badge {
  font-size: 10.5px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 1px 7px;
  border-radius: 999px;
}

.brand-subtitle {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 1px;
}

.nav-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
}

.nav-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  color: #475569;
  font-size: 13.5px;
  font-weight: 500;
  padding: 7px 14px;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.nav-tab-btn:hover {
  color: #0f172a;
}

.nav-tab-btn.active {
  background: #ffffff;
  color: #2563eb;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.nav-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 5px 12px;
  border-radius: 999px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

.status-dot.offline {
  background: #f59e0b;
}

.main-container {
  flex: 1;
  width: 100%;
  max-width: 1480px;
  margin: 0 auto;
  padding: 20px 24px 32px;
}

.app-footer {
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
  padding: 12px 24px;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
