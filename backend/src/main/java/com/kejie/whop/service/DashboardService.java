package com.kejie.whop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kejie.whop.mapper.AttendanceStatisticsMapper;
import com.kejie.whop.mapper.OutboundOrderMapper;
import com.kejie.whop.mapper.QuotationInfoMapper;
import com.kejie.whop.mapper.WorkloadStatisticsDetailMapper;
import com.kejie.whop.model.entity.AttendanceStatistics;
import com.kejie.whop.model.entity.OutboundOrder;
import com.kejie.whop.model.entity.QuotationInfo;
import com.kejie.whop.model.entity.WorkloadStatisticsDetail;
import com.kejie.whop.model.vo.DashboardOverviewVO;
import com.kejie.whop.model.vo.TrendDataVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final AttendanceStatisticsMapper attendanceStatisticsMapper;
    private final QuotationInfoMapper quotationInfoMapper;
    private final WorkloadStatisticsDetailMapper workloadStatisticsDetailMapper;
    private final WarehouseService warehouseService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");

    public DashboardOverviewVO getOverview(String warehouseCode, String month) {
        DashboardOverviewVO vo = new DashboardOverviewVO();

        // 1. 出库单聚合：总单量 & 总件数
        QueryWrapper<OutboundOrder> orderQw = new QueryWrapper<>();
        orderQw.select("COUNT(*) as totalOrders",
                "SUM(CAST(物料总数量 AS DECIMAL(20,2))) as totalItems",
                "库房编码", "库房名称");
        if (warehouseCode != null && !warehouseCode.isEmpty()) {
            orderQw.eq("库房编码", warehouseCode);
        }
        if (month != null && !month.isEmpty()) {
            orderQw.apply("DATE_FORMAT(创建时间, '%Y-%m') = {0}", month);
        }
        orderQw.groupBy("库房编码", "库房名称");
        List<Map<String, Object>> orderRows = outboundOrderMapper.selectMaps(orderQw);

        long totalOrders = 0;
        long totalItems = 0;
        String whCode = warehouseCode;
        String whName = "";
        for (Map<String, Object> row : orderRows) {
            if (row == null) continue;
            totalOrders += toLong(row.get("totalOrders"));
            totalItems += toLong(row.get("totalItems"));
            if (whCode == null || whCode.isEmpty()) {
                whCode = String.valueOf(row.get("库房编码"));
            }
            whName = String.valueOf(row.get("库房名称"));
        }

        // 2. 出勤统计聚合：总工时 & 日均人数 & 工作天数
        String warehouseName = warehouseCode != null ? warehouseService.getWarehouseName(warehouseCode) : null;
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        QueryWrapper<AttendanceStatistics> attQw = new QueryWrapper<>();
        attQw.select("SUM(CAST(工作时长 AS DECIMAL(10,2))) as totalWorkHours",
                "COUNT(DISTINCT 员工编码) as distinctEmployees",
                "COUNT(DISTINCT 考勤日期) as workDays");
        if (attWarehouseName != null) {
            attQw.eq("库房", attWarehouseName);
        }
        if (month != null && !month.isEmpty()) {
            attQw.apply("DATE_FORMAT(考勤日期, '%Y-%m') = {0}", month);
        }
        List<Map<String, Object>> attRows = attendanceStatisticsMapper.selectMaps(attQw);

        BigDecimal totalWorkHours = BigDecimal.ZERO;
        int workDays = 1;
        int avgHeadcount = 0;
        if (!attRows.isEmpty() && attRows.get(0) != null) {
            Map<String, Object> att = attRows.get(0);
            totalWorkHours = toBigDecimal(att.get("totalWorkHours"));
            workDays = Math.max(1, toInt(att.get("workDays")));
            int distinctEmployees = toInt(att.get("distinctEmployees"));
            avgHeadcount = workDays > 0 ? distinctEmployees : 0;
        }

        // 3. 报价信息获取：加权平均单价
        BigDecimal weightedUnitPrice = getWeightedUnitPrice(warehouseName);

        // 4. 计算费用
        BigDecimal monthlyFee = totalWorkHours
                .multiply(weightedUnitPrice)
                .multiply(BigDecimal.ONE.add(TAX_RATE))
                .setScale(2, RoundingMode.HALF_UP);

        // 5. 计算人效、单均成本、件均成本
        BigDecimal laborEfficiency = BigDecimal.ZERO;
        if (avgHeadcount > 0 && workDays > 0) {
            laborEfficiency = BigDecimal.valueOf(totalOrders)
                    .divide(BigDecimal.valueOf((long) avgHeadcount * workDays), 2, RoundingMode.HALF_UP);
        }
        BigDecimal avgCostPerOrder = totalOrders > 0
                ? monthlyFee.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgCostPerItem = totalItems > 0
                ? monthlyFee.divide(BigDecimal.valueOf(totalItems), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        vo.setWarehouseCode(whCode);
        vo.setWarehouseName(whName);
        vo.setMonth(month);
        vo.setTotalOrders(totalOrders);
        vo.setTotalItems(totalItems);
        vo.setTotalWorkHours(totalWorkHours);
        vo.setMonthlyFee(monthlyFee);
        vo.setLaborEfficiency(laborEfficiency);
        vo.setAvgCostPerOrder(avgCostPerOrder);
        vo.setAvgCostPerItem(avgCostPerItem);
        vo.setAvgHeadcount(avgHeadcount);
        vo.setWorkDays(workDays);
        vo.setDailyAvgFee(workDays > 0
                ? monthlyFee.divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setItemsPerOrder(totalOrders > 0
                ? BigDecimal.valueOf(totalItems).divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return vo;
    }

    public List<TrendDataVO> getTrend(String warehouseCode, String startMonth, String endMonth, String type) {
        if ("outbound_orders".equals(type)) {
            return getOutboundTrend(warehouseCode, startMonth, endMonth);
        } else if ("fee".equals(type)) {
            return getFeeTrend(warehouseCode, startMonth, endMonth);
        } else if ("workload_distribution".equals(type)) {
            return getWorkloadTrend(warehouseCode, startMonth, endMonth);
        } else if ("monthly_fee_breakdown".equals(type)) {
            return getFeeBreakdownTrend(warehouseCode, startMonth, endMonth);
        }
        return new ArrayList<>();
    }

    private List<TrendDataVO> getOutboundTrend(String warehouseCode, String startMonth, String endMonth) {
        QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();
        qw.select("DATE(创建时间) as dt", "COUNT(*) as cnt", "库房编码", "库房名称");
        if (warehouseCode != null && !warehouseCode.isEmpty()) {
            qw.eq("库房编码", warehouseCode);
        }
        applyMonthRange(qw, "创建时间", startMonth, endMonth);
        qw.groupBy("DATE(创建时间)", "库房编码", "库房名称")
                .orderByAsc("DATE(创建时间)");

        List<Map<String, Object>> rows = outboundOrderMapper.selectMaps(qw);
        return rows.stream().filter(Objects::nonNull).map(row -> {
            TrendDataVO vo = new TrendDataVO();
            vo.setDate(String.valueOf(row.get("dt")));
            vo.setWarehouseCode(String.valueOf(row.get("库房编码")));
            vo.setWarehouseName(String.valueOf(row.get("库房名称")));
            vo.setValue(toBigDecimal(row.get("cnt")));
            vo.setType("outbound_orders");
            return vo;
        }).toList();
    }

    private List<TrendDataVO> getFeeTrend(String warehouseCode, String startMonth, String endMonth) {
        // 按月聚合工时，再计算费用
        String warehouseName = warehouseCode != null ? warehouseService.getWarehouseName(warehouseCode) : null;
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);

        QueryWrapper<AttendanceStatistics> qw = new QueryWrapper<>();
        qw.select("DATE_FORMAT(考勤日期, '%Y-%m') as ym",
                "SUM(CAST(工作时长 AS DECIMAL(10,2))) as totalHours", "库房");
        if (attWarehouseName != null) {
            qw.eq("库房", attWarehouseName);
        }
        applyMonthRangeAtt(qw, startMonth, endMonth);
        qw.groupBy("DATE_FORMAT(考勤日期, '%Y-%m')", "库房")
                .orderByAsc("DATE_FORMAT(考勤日期, '%Y-%m')");

        List<Map<String, Object>> rows = attendanceStatisticsMapper.selectMaps(qw);
        BigDecimal unitPrice = getWeightedUnitPrice(warehouseName);

        return rows.stream().filter(Objects::nonNull).map(row -> {
            TrendDataVO vo = new TrendDataVO();
            vo.setDate(String.valueOf(row.get("ym")));
            vo.setWarehouseCode(warehouseCode != null ? warehouseCode : "");
            vo.setWarehouseName(String.valueOf(row.get("库房")));
            BigDecimal hours = toBigDecimal(row.get("totalHours"));
            BigDecimal fee = hours.multiply(unitPrice).multiply(BigDecimal.ONE.add(TAX_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setValue(fee);
            vo.setType("fee");
            return vo;
        }).toList();
    }

    private List<TrendDataVO> getWorkloadTrend(String warehouseCode, String startMonth, String endMonth) {
        // 按操作类型聚合操作记录数，数据来源：工作量统计操作明细表
        QueryWrapper<WorkloadStatisticsDetail> qw = new QueryWrapper<>();
        qw.select("操作类型 as opType", "COUNT(*) as cnt");
        qw.isNotNull("操作类型");
        applyMonthRangeDetail(qw, startMonth, endMonth);
        qw.groupBy("操作类型")
                .orderByDesc("cnt");

        List<Map<String, Object>> rows = workloadStatisticsDetailMapper.selectMaps(qw);
        return rows.stream().filter(Objects::nonNull).map(row -> {
            TrendDataVO vo = new TrendDataVO();
            vo.setDate(startMonth != null ? startMonth : "");
            vo.setWarehouseCode(warehouseCode != null ? warehouseCode : "");
            vo.setWarehouseName("");
            vo.setValue(toBigDecimal(row.get("cnt")));
            vo.setType(String.valueOf(row.get("opType")));
            return vo;
        }).toList();
    }

    private List<TrendDataVO> getFeeBreakdownTrend(String warehouseCode, String startMonth, String endMonth) {
        // 按员工类型拆分月度费用构成
        String warehouseName = warehouseCode != null ? warehouseService.getWarehouseName(warehouseCode) : null;
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);

        QueryWrapper<AttendanceStatistics> qw = new QueryWrapper<>();
        qw.select("DATE_FORMAT(考勤日期, '%Y-%m') as ym",
                "员工类型 as empType",
                "SUM(CAST(工作时长 AS DECIMAL(10,2))) as totalHours",
                "库房");
        if (attWarehouseName != null) {
            qw.eq("库房", attWarehouseName);
        }
        applyMonthRangeAtt(qw, startMonth, endMonth);
        qw.groupBy("DATE_FORMAT(考勤日期, '%Y-%m')", "员工类型", "库房")
                .orderByAsc("DATE_FORMAT(考勤日期, '%Y-%m')");

        List<Map<String, Object>> rows = attendanceStatisticsMapper.selectMaps(qw);
        BigDecimal unitPrice = getWeightedUnitPrice(warehouseName);

        return rows.stream().filter(Objects::nonNull).map(row -> {
            TrendDataVO vo = new TrendDataVO();
            vo.setDate(String.valueOf(row.get("ym")));
            vo.setWarehouseCode(warehouseCode != null ? warehouseCode : "");
            vo.setWarehouseName(String.valueOf(row.get("库房")));
            BigDecimal hours = toBigDecimal(row.get("totalHours"));
            BigDecimal fee = hours.multiply(unitPrice).multiply(BigDecimal.ONE.add(TAX_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setValue(fee);
            vo.setType(String.valueOf(row.get("empType")));
            return vo;
        }).toList();
    }

    // --- 工具方法 ---

    BigDecimal getWeightedUnitPrice(String warehouseName) {
        if (warehouseName == null) {
            return new BigDecimal("20.00");
        }
        QueryWrapper<QuotationInfo> qw = new QueryWrapper<>();
        qw.eq("库房名称", warehouseName)
                .isNotNull("供应商结算单价")
                .orderByDesc("报价开始日期");
        List<Map<String, Object>> rows = quotationInfoMapper.selectMaps(
                new QueryWrapper<QuotationInfo>()
                        .select("AVG(供应商结算单价) as avgPrice")
                        .eq("库房名称", warehouseName)
                        .isNotNull("供应商结算单价"));
        if (rows.isEmpty() || rows.get(0) == null || rows.get(0).get("avgPrice") == null) {
            return new BigDecimal("20.00");
        }
        return toBigDecimal(rows.get(0).get("avgPrice")).setScale(2, RoundingMode.HALF_UP);
    }

    private <T> void applyMonthRange(QueryWrapper<T> qw, String dateCol, String startMonth, String endMonth) {
        if (startMonth != null && !startMonth.isEmpty()) {
            qw.apply("DATE_FORMAT(" + dateCol + ", '%Y-%m') >= {0}", startMonth);
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            qw.apply("DATE_FORMAT(" + dateCol + ", '%Y-%m') <= {0}", endMonth);
        }
    }

    private void applyMonthRangeAtt(QueryWrapper<AttendanceStatistics> qw, String startMonth, String endMonth) {
        if (startMonth != null && !startMonth.isEmpty()) {
            qw.apply("DATE_FORMAT(考勤日期, '%Y-%m') >= {0}", startMonth);
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            qw.apply("DATE_FORMAT(考勤日期, '%Y-%m') <= {0}", endMonth);
        }
    }

    private void applyMonthRangeDetail(QueryWrapper<WorkloadStatisticsDetail> qw, String startMonth, String endMonth) {
        if (startMonth != null && !startMonth.isEmpty()) {
            qw.apply("DATE_FORMAT(操作时间, '%Y-%m') >= {0}", startMonth);
        }
        if (endMonth != null && !endMonth.isEmpty()) {
            qw.apply("DATE_FORMAT(操作时间, '%Y-%m') <= {0}", endMonth);
        }
    }

    static long toLong(Object val) {
        if (val == null) return 0;
        return new BigDecimal(val.toString()).longValue();
    }

    static int toInt(Object val) {
        if (val == null) return 0;
        return new BigDecimal(val.toString()).intValue();
    }

    static BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
