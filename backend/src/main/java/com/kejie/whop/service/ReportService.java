package com.kejie.whop.service;

import com.kejie.whop.model.dto.ReportGenerateRequest;
import com.kejie.whop.model.vo.DashboardOverviewVO;
import com.kejie.whop.model.vo.MonthlyBaselineVO;
import com.kejie.whop.model.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final DashboardService dashboardService;
    private final BaselineService baselineService;
    private final WarehouseService warehouseService;
    private final ImpactService impactService;

    private final Map<String, ReportVO> reportStore = new ConcurrentHashMap<>();

    public ReportVO generate(ReportGenerateRequest request) {
        String warehouseCode = request.getWarehouseCode();
        String warehouseName = warehouseService.getWarehouseName(warehouseCode);
        if (warehouseName == null) {
            warehouseName = warehouseCode;
        }

        String startMonth = request.getStartMonth();
        String endMonth = request.getEndMonth();

        // 收集数据
        StringBuilder md = new StringBuilder();
        md.append("# ").append(warehouseName).append(" 费用分析报告\n\n");
        md.append("**报告时间范围**: ").append(startMonth).append(" ~ ").append(endMonth).append("\n\n");
        md.append("**生成时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        // 概览
        md.append("## 一、概览\n\n");
        DashboardOverviewVO overview = dashboardService.getOverview(warehouseCode, startMonth);
        md.append("| 指标 | 数值 |\n|---|---|\n");
        md.append("| 总出库单量 | ").append(overview.getTotalOrders()).append(" |\n");
        md.append("| 总出库件数 | ").append(overview.getTotalItems()).append(" |\n");
        md.append("| 件单比 | ").append(overview.getItemsPerOrder()).append(" |\n");
        md.append("| 总工时(h) | ").append(overview.getTotalWorkHours()).append(" |\n");
        md.append("| 月度操作费用(元) | ").append(overview.getMonthlyFee()).append(" |\n");
        md.append("| 日均费用(元) | ").append(overview.getDailyAvgFee()).append(" |\n");
        md.append("| 出勤人数 | ").append(overview.getAvgHeadcount()).append(" |\n");
        md.append("| 人效(单/人/天) | ").append(overview.getLaborEfficiency()).append(" |\n");
        md.append("| 单均成本(元) | ").append(overview.getAvgCostPerOrder()).append(" |\n");
        md.append("| 件均成本(元) | ").append(overview.getAvgCostPerItem()).append(" |\n\n");

        // 月度基线明细
        md.append("## 二、月度费用基线\n\n");
        md.append("### 计算公式\n\n");
        md.append("月度劳务费用 = 月总工时(h) × 加权单价(元/h) × (1 + 税率6%)\n\n");
        md.append("| 月份 | 仓库类型 | 总费用(元) | 日均费用(元) | 总单量 | 件单比 | 单均成本 | 件均成本 | 人效(单/人/天) |\n");
        md.append("|---|---|---|---|---|---|---|---|---|\n");

        DateTimeFormatter ymf = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth start = YearMonth.parse(startMonth, ymf);
        YearMonth end = YearMonth.parse(endMonth, ymf);
        YearMonth current = start;
        while (!current.isAfter(end)) {
            String currentMonth = current.toString();
            List<MonthlyBaselineVO> baselines = baselineService.getMonthlyBaseline(
                warehouseCode, currentMonth, currentMonth);
            for (MonthlyBaselineVO b : baselines) {
                md.append("| ").append(current).append(" | ");
                md.append(b.getWarehouseType() != null ? b.getWarehouseType() : "-").append(" | ");
                md.append(b.getTotalFee()).append(" | ");
                md.append(b.getDailyAvgFee()).append(" | ");
                md.append(b.getTotalOrders()).append(" | ");
                md.append(b.getItemsPerOrder()).append(" | ");
                md.append(b.getCostPerOrder()).append(" | ");
                md.append(b.getCostPerItem()).append(" | ");
                md.append(b.getLaborEfficiency()).append(" |\n");
            }
            current = current.plusMonths(1);
        }

        // 影响因素三梯队
        md.append("\n## 三、影响因素分析（三梯队）\n\n");
        try {
            var factors = impactService.getFactors(warehouseCode);
            md.append("| 排名 | 因素 | 相关系数 | 梯队 | 应用建议 |\n");
            md.append("|---|---|---|---|---|\n");
            for (var f : factors) {
                md.append("| ").append(f.getRank()).append(" | ");
                md.append(f.getFactorName()).append(" | ");
                md.append(f.getCorrelation()).append(" | ");
                md.append(f.getTierLabel() != null ? f.getTierLabel() : "-").append(" | ");
                md.append(f.getApplicationTip() != null ? f.getApplicationTip() : "-").append(" |\n");
            }
        } catch (Exception e) {
            md.append("影响因素数据暂不可用。\n");
        }

        md.append("\n## 四、测算公式参考\n\n");
        md.append("**五步测算法**: 业务量输入 → 人力需求推算 → 工时计算 → 费用计算 → 报价输出\n\n");
        md.append("- 人力需求: N = ⌈日均单量 / 人效⌉\n");
        md.append("- 件单比修正: K = 1 + 0.05 × max(0, 目标件单比 - 基线件单比)\n");
        md.append("- 月度总工时: H = N × K × 人均日工时 × 工作天数\n");
        md.append("- 加权单价: P = α×固定 + β×临时 + γ×叉车\n");
        md.append("- 月度费用: F = H × P × (1 + 6%)\n\n");

        // 生成报告对象
        String id = "rpt-" + UUID.randomUUID().toString().substring(0, 8);
        String title = warehouseName + " 费用分析报告 (" + startMonth + " ~ " + endMonth + ")";

        ReportVO report = new ReportVO();
        report.setId(id);
        report.setTitle(title);
        report.setWarehouseCode(warehouseCode);
        report.setWarehouseName(warehouseName);
        report.setStartMonth(startMonth);
        report.setEndMonth(endMonth);
        report.setContent(md.toString());
        report.setCreatedAt(LocalDateTime.now());

        reportStore.put(id, report);

        // 返回不含 content 的摘要
        ReportVO summary = new ReportVO();
        summary.setId(id);
        summary.setTitle(title);
        summary.setWarehouseCode(warehouseCode);
        summary.setWarehouseName(warehouseName);
        summary.setStartMonth(startMonth);
        summary.setEndMonth(endMonth);
        summary.setCreatedAt(report.getCreatedAt());
        return summary;
    }

    public List<ReportVO> list() {
        return reportStore.values().stream()
                .sorted(Comparator.comparing(ReportVO::getCreatedAt).reversed())
                .map(r -> {
                    ReportVO summary = new ReportVO();
                    summary.setId(r.getId());
                    summary.setTitle(r.getTitle());
                    summary.setWarehouseCode(r.getWarehouseCode());
                    summary.setWarehouseName(r.getWarehouseName());
                    summary.setStartMonth(r.getStartMonth());
                    summary.setEndMonth(r.getEndMonth());
                    summary.setCreatedAt(r.getCreatedAt());
                    return summary;
                })
                .toList();
    }

    public ReportVO getById(String id) {
        return reportStore.get(id);
    }
}
