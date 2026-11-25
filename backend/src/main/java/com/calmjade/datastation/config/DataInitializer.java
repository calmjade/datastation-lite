package com.calmjade.datastation.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Idempotently initializes schema and realistic Chinese business seed data on startup.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTablesIfNotExist();
        seedSalesOrdersIfEmpty();
        seedChannelKpiIfEmpty();
        seedReportsIfEmpty();
    }

    private void createTablesIfNotExist() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS biz_sales_orders (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    order_no VARCHAR(40) NOT NULL,
                    customer_name VARCHAR(100) NOT NULL,
                    region VARCHAR(30) NOT NULL,
                    product_line VARCHAR(60) NOT NULL,
                    channel VARCHAR(30) NOT NULL,
                    amount DECIMAL(14, 2) NOT NULL,
                    profit DECIMAL(14, 2) NOT NULL,
                    order_date DATE NOT NULL,
                    status VARCHAR(20) NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS biz_channel_kpi (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    kpi_month VARCHAR(10) NOT NULL,
                    channel VARCHAR(30) NOT NULL,
                    leads INT NOT NULL,
                    signed_deals INT NOT NULL,
                    conversion_rate DECIMAL(6, 2) NOT NULL,
                    cost DECIMAL(14, 2) NOT NULL,
                    revenue DECIMAL(14, 2) NOT NULL,
                    roi DECIMAL(8, 2) NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ds_report_config (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(120) NOT NULL,
                    category VARCHAR(60) NOT NULL,
                    description VARCHAR(500),
                    sql_text TEXT NOT NULL,
                    columns_json TEXT,
                    chart_config_json TEXT,
                    transform_script TEXT,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
    }

    private void seedSalesOrdersIfEmpty() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_sales_orders", Long.class);
        if (count != null && count > 0) {
            return;
        }

        String[] regions = {"华东", "华北", "华南", "西南"};
        String[] productLines = {"智慧供应链SaaS", "低代码报表引擎", "企业数据中台", "AI客服坐席"};
        String[] channels = {"直客", "渠道代理", "云市场"};
        String[] statuses = {"已完成", "回款中", "实施中"};
        String[] customers = {
                "沪上智造科技", "京城云数联", "粤海精密集团", "蜀道数智物流",
                "江南新零售", "津门航运信息", "鹏城创新金融", "蓉城医疗健康",
                "苏宁易链网络", "中关村数科", "广汽数字生态", "渝新欧供应链",
                "杭城电商云", "冀东能源集团", "佛山智能家居", "昆仑智算中心"
        };

        String insertSql = """
                INSERT INTO biz_sales_orders
                (order_no, customer_name, region, product_line, channel, amount, profit, order_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        // Generate 48 realistic orders spanning Jan 2026 to Nov 2026
        for (int i = 0; i < 48; i++) {
            String orderNo = String.format("SO-2026-%04d", 1001 + i);
            String customer = customers[i % customers.length];
            String region = regions[(i * 3 + i / 4) % regions.length];
            String productLine = productLines[(i + i / 3) % productLines.length];
            String channel = channels[(i * 2 + 1) % channels.length];
            String status = statuses[(i % 7 == 0) ? 2 : (i % 3 == 0 ? 1 : 0)];

            int month = (i % 11) + 1; // 1..11
            int day = ((i * 5) % 26) + 2;
            LocalDate orderDate = LocalDate.of(2026, month, day);

            // Base amount varies realistically by product line and region
            double regionFactor = switch (region) {
                case "华东" -> 1.25;
                case "华北" -> 1.12;
                case "华南" -> 1.05;
                default -> 0.88;
            };
            double productBase = switch (productLine) {
                case "企业数据中台" -> 285000.0;
                case "智慧供应链SaaS" -> 198000.0;
                case "低代码报表引擎" -> 142000.0;
                default -> 116000.0;
            };
            double variance = 0.78 + ((i * 17) % 45) / 100.0;
            BigDecimal amount = BigDecimal.valueOf(productBase * regionFactor * variance)
                    .setScale(2, RoundingMode.HALF_UP);

            double marginRate = switch (productLine) {
                case "低代码报表引擎" -> 0.64;
                case "AI客服坐席" -> 0.58;
                case "智慧供应链SaaS" -> 0.52;
                default -> 0.46;
            };
            double channelPenalty = "渠道代理".equals(channel) ? -0.06 : ("云市场".equals(channel) ? -0.03 : 0.02);
            BigDecimal profit = amount.multiply(BigDecimal.valueOf(marginRate + channelPenalty))
                    .setScale(2, RoundingMode.HALF_UP);

            jdbcTemplate.update(
                    insertSql,
                    orderNo,
                    customer,
                    region,
                    productLine,
                    channel,
                    amount,
                    profit,
                    Date.valueOf(orderDate),
                    status
            );
        }
    }

    private void seedChannelKpiIfEmpty() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_channel_kpi", Long.class);
        if (count != null && count > 0) {
            return;
        }

        List<String> months = List.of("2026-06", "2026-07", "2026-08", "2026-09", "2026-10", "2026-11");
        String[] channels = {"直客", "渠道代理", "云市场"};

        String insertSql = """
                INSERT INTO biz_channel_kpi
                (kpi_month, channel, leads, signed_deals, conversion_rate, cost, revenue, roi)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        for (int m = 0; m < months.size(); m++) {
            String month = months.get(m);
            for (int c = 0; c < channels.length; c++) {
                String channel = channels[c];
                int leads = switch (channel) {
                    case "直客" -> 185 + m * 14;
                    case "渠道代理" -> 140 + m * 11;
                    default -> 230 + m * 18;
                };
                int deals = switch (channel) {
                    case "直客" -> 22 + m * 2;
                    case "渠道代理" -> 19 + m * 2;
                    default -> 16 + m;
                };
                BigDecimal conversion = BigDecimal.valueOf(deals * 100.0 / leads).setScale(2, RoundingMode.HALF_UP);
                BigDecimal cost = BigDecimal.valueOf(switch (channel) {
                    case "直客" -> 92000.0 + m * 4500;
                    case "渠道代理" -> 76000.0 + m * 3200;
                    default -> 64000.0 + m * 2800;
                }).setScale(2, RoundingMode.HALF_UP);
                BigDecimal revenue = BigDecimal.valueOf(switch (channel) {
                    case "直客" -> 540000.0 + m * 48000;
                    case "渠道代理" -> 395000.0 + m * 36000;
                    default -> 285000.0 + m * 26000;
                }).setScale(2, RoundingMode.HALF_UP);
                BigDecimal roi = revenue.divide(cost, 2, RoundingMode.HALF_UP);

                jdbcTemplate.update(insertSql, month, channel, leads, deals, conversion, cost, revenue, roi);
            }
        }
    }

    private void seedReportsIfEmpty() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ds_report_config", Long.class);
        if (count != null && count > 0) {
            return;
        }

        String insertSql = """
                INSERT INTO ds_report_config
                (name, category, description, sql_text, columns_json, chart_config_json, transform_script, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        // 1. Bar chart: Region revenue & profit
        jdbcTemplate.update(
                insertSql,
                "各区域营收与毛利对比",
                "经营分析",
                "按销售大区统计累计合同金额、净利润及综合毛利率表现",
                """
                SELECT region,
                       COUNT(*) AS order_count,
                       ROUND(SUM(amount), 2) AS total_amount,
                       ROUND(SUM(profit), 2) AS total_profit,
                       ROUND(SUM(profit) * 100.0 / SUM(amount), 2) AS margin_rate
                FROM biz_sales_orders
                GROUP BY region
                ORDER BY total_amount DESC
                """.stripIndent().trim(),
                """
                [
                  {"key":"region","label":"销售区域","formatter":"tag"},
                  {"key":"order_count","label":"订单数","formatter":"number"},
                  {"key":"total_amount","label":"总营收","formatter":"currency"},
                  {"key":"total_profit","label":"总毛利","formatter":"currency"},
                  {"key":"margin_rate","label":"毛利率","formatter":"percent"}
                ]
                """.stripIndent().trim(),
                """
                {
                  "chartType": "bar",
                  "xField": "region",
                  "yFields": ["total_amount", "total_profit"],
                  "palette": "business",
                  "smooth": true,
                  "showLabel": false
                }
                """.stripIndent().trim(),
                "",
                now
        );

        // 2. Line chart: Monthly sales & profit trend
        jdbcTemplate.update(
                insertSql,
                "2026年月度营收与利润趋势",
                "财务看板",
                "追踪 2026 年各月度签约总额与毛利增长曲线",
                """
                SELECT SUBSTRING(CAST(order_date AS VARCHAR), 1, 7) AS order_month,
                       COUNT(*) AS order_count,
                       ROUND(SUM(amount), 2) AS total_amount,
                       ROUND(SUM(profit), 2) AS total_profit
                FROM biz_sales_orders
                GROUP BY SUBSTRING(CAST(order_date AS VARCHAR), 1, 7)
                ORDER BY order_month ASC
                """.stripIndent().trim(),
                """
                [
                  {"key":"order_month","label":"月份","formatter":"default"},
                  {"key":"order_count","label":"订单量","formatter":"number"},
                  {"key":"total_amount","label":"月度营收","formatter":"currency"},
                  {"key":"total_profit","label":"月度毛利","formatter":"currency"}
                ]
                """.stripIndent().trim(),
                """
                {
                  "chartType": "line",
                  "xField": "order_month",
                  "yFields": ["total_amount", "total_profit"],
                  "palette": "aurora",
                  "smooth": true,
                  "showLabel": false
                }
                """.stripIndent().trim(),
                "",
                now
        );

        // 3. Pie chart: Product line revenue share
        jdbcTemplate.update(
                insertSql,
                "产品线营收结构占比",
                "产品运营",
                "对比四大核心产品线的市场营收贡献及订单分布",
                """
                SELECT product_line,
                       COUNT(*) AS order_count,
                       ROUND(SUM(amount), 2) AS total_amount,
                       ROUND(SUM(profit), 2) AS total_profit
                FROM biz_sales_orders
                GROUP BY product_line
                ORDER BY total_amount DESC
                """.stripIndent().trim(),
                """
                [
                  {"key":"product_line","label":"产品线","formatter":"tag"},
                  {"key":"order_count","label":"成交单数","formatter":"number"},
                  {"key":"total_amount","label":"产品营收","formatter":"currency"},
                  {"key":"total_profit","label":"产品毛利","formatter":"currency"}
                ]
                """.stripIndent().trim(),
                """
                {
                  "chartType": "pie",
                  "xField": "product_line",
                  "yFields": ["total_amount"],
                  "palette": "emerald",
                  "smooth": false,
                  "showLabel": true
                }
                """.stripIndent().trim(),
                "",
                now
        );

        // 4. Radar chart: Channel KPI & ROI evaluation
        jdbcTemplate.update(
                insertSql,
                "渠道获客效能与 ROI 评估",
                "营销获客",
                "从线索规模、签约转化与投入产出比三维评估各渠道综合效能",
                """
                SELECT channel,
                       SUM(leads) AS total_leads,
                       SUM(signed_deals) AS total_deals,
                       ROUND(AVG(conversion_rate), 2) AS avg_conv_rate,
                       ROUND(SUM(cost), 2) AS total_cost,
                       ROUND(AVG(roi), 2) AS avg_roi
                FROM biz_channel_kpi
                GROUP BY channel
                ORDER BY avg_roi DESC
                """.stripIndent().trim(),
                """
                [
                  {"key":"channel","label":"获客渠道","formatter":"tag"},
                  {"key":"total_leads","label":"累计线索","formatter":"number"},
                  {"key":"total_deals","label":"累计签约","formatter":"number"},
                  {"key":"avg_conv_rate","label":"平均转化率","formatter":"percent"},
                  {"key":"total_cost","label":"投放成本","formatter":"currency"},
                  {"key":"avg_roi","label":"平均ROI","formatter":"number"}
                ]
                """.stripIndent().trim(),
                """
                {
                  "chartType": "radar",
                  "xField": "channel",
                  "yFields": ["total_leads", "total_deals", "avg_roi"],
                  "palette": "sunset",
                  "smooth": false,
                  "showLabel": false
                }
                """.stripIndent().trim(),
                "",
                now
        );
    }
}
