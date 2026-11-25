# 数据驿站 Lite（DataStation Lite）

**配置化低代码报表与 BI 看板平台**

数据驿站 Lite 是一款面向企业日常经营分析的轻量级、配置化低代码报表与 BI 数据看板平台。支持通过只读 SQL 探索业务数据、可视化配置 ECharts 图表（柱状图 / 折线图 / 饼图 / 雷达图）与字段格式化规则（金额、百分比、状态标签），并一键发布至驾驶舱门户进行实时渲染与 CSV 导出。

---

## 核心特性

- **驾驶舱门户（BI Portal）**
  - 顶部核心经营 KPI 概览卡片（总营收、累计利润、订单总量、平均渠道 ROI）
  - 卡片式 BI 报表网格，支持 ECharts 实时可视化渲染与明细数据表格切换
  - 支持分类筛选、单卡刷新、全局刷新以及一键导出带 UTF-8 BOM 的 CSV 文件
- **低代码报表设计器（Report Studio）**
  - 左侧表结构与字段树浏览，支持快捷插入 SQL 片段与预设业务查询模板
  - 内置只读 SQL 编辑器（支持 `Ctrl+Enter` 快捷运行，实时展示执行耗时与行数）
  - 右侧可视化配置面板：字段别名与格式化（金额 / 百分比 / 状态标签）、图表类型（`bar` / `line` / `pie` / `radar`）、X 轴维度、Y 轴多指标、企业级配色方案
  - 实时联动预览，一键保存并发布至驾驶舱
- **数据源与表字典（Dataset Explorer）**
  - 自动探测 H2 业务表元数据（表名、行数、字段类型、示例值）
  - 一键生成 `SELECT` 查询语句并跳转至低代码报表设计器
- **SQL 安全防线**
  - 严格拦截非只读语句，仅允许 `SELECT` / `WITH` 查询，自动拒绝 `DROP`、`DELETE`、`UPDATE`、`INSERT`、`ALTER`、`TRUNCATE`、`CREATE`、`MERGE`、`EXEC` 及多语句 `;` 注入，单次最多返回 500 行。

---

## 技术栈与端口约定

| 模块 | 技术栈 | 默认端口 |
| --- | --- | --- |
| 后端服务 (`backend/`) | Java 21 + Spring Boot 3.5.3 + Spring Data JPA + JDBC + H2 (MySQL Mode) | `8091` |
| 前端应用 (`frontend/`) | Vue 3.5 + TypeScript + Vite 6 + Element Plus + ECharts + Pinia | `9021` |

---

## 快速启动

### 1. 启动后端服务（端口 8091）

在项目根目录执行启动脚本（脚本会自动配置 JDK 21 与 Maven 3.9.9 环境变量）：

```powershell
.\start-backend.ps1
```

后端启动后会自动初始化 H2 文件数据库（`./data/datastation`）及预置演示数据：
- `biz_sales_orders`：48 条多区域、多产品线、多渠道的销售订单明细
- `biz_channel_kpi`：18 条月度渠道投放、线索、签约与 ROI 指标数据
- `ds_report_config`：4 个开箱即用的可视化 BI 报表配置

### 2. 启动前端应用（端口 9021）

```powershell
cd frontend
pnpm install
pnpm dev
```

访问地址：`http://localhost:9021`（Vite 已自动配置 `/api` 代理至 `http://localhost:8091`）。

---

## 后端 API 一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/health` | 服务健康检查、已发布报表数与可用表清单 |
| `GET` | `/api/schema/tables` | 获取业务表元数据、行数、字段类型及示例数据 |
| `POST` | `/api/query/execute` | 执行受保护的只读 SQL 查询（最多返回 500 行） |
| `GET` | `/api/reports` | 查询所有已配置报表 |
| `GET` | `/api/reports/{id}` | 获取单张报表详情 |
| `POST` | `/api/reports` | 新增并发布低代码报表 |
| `PUT` | `/api/reports/{id}` | 更新报表配置 |
| `DELETE` | `/api/reports/{id}` | 删除报表 |
| `POST` | `/api/reports/{id}/run` | 执行指定报表 SQL 并返回结构化数据与耗时 |

---

## 作者信息

- **作者**：曹杰 / caojie (`calmjade`)
- **邮箱**：calmjade@foxmail.com
- **GitHub**：[https://github.com/calmjade](https://github.com/calmjade)
- **Gitee**：[https://gitee.com/jiejie12315](https://gitee.com/jiejie12315)
- **开源协议**：[MIT License](./LICENSE)
