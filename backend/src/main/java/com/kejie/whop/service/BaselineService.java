package com.kejie.whop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kejie.whop.mapper.AttendanceStatisticsMapper;
import com.kejie.whop.mapper.OutboundOrderMapper;
import com.kejie.whop.mapper.WorkloadStatisticsDetailMapper;
import com.kejie.whop.mapper.WorkloadStatisticsInfoMapper;
import com.kejie.whop.model.entity.AttendanceStatistics;
import com.kejie.whop.model.entity.OutboundOrder;
import com.kejie.whop.model.entity.WorkloadStatisticsDetail;
import com.kejie.whop.model.entity.WorkloadStatisticsInfo;
import com.kejie.whop.model.vo.CompareResultVO;
import com.kejie.whop.model.vo.DailyDetailVO;
import com.kejie.whop.model.vo.MonthlyBaselineVO;
import com.kejie.whop.model.vo.OperationFeeDetailVO;
import com.kejie.whop.model.vo.WarehouseDetailVO;
import com.kejie.whop.model.vo.WarehouseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.kejie.whop.service.DashboardService.*;

@Service
@RequiredArgsConstructor
public class BaselineService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final AttendanceStatisticsMapper attendanceStatisticsMapper;
    private final WorkloadStatisticsInfoMapper workloadStatisticsInfoMapper;
    private final WorkloadStatisticsDetailMapper workloadStatisticsDetailMapper;
    private final WarehouseService warehouseService;
    private final DashboardService dashboardService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");

    /**
     * SQL CASE expression: derive normalized 操作大类 from raw 操作大类 + 单据类型.
     * - Merges 手持出库→出库, 手持入库→入库, 手持在库→在库
     * - When 操作大类 IS NULL, falls back to 单据类型 based classification
     */
    private static final String OP_CATEGORY_CASE =
        "CASE " +
        "WHEN 操作大类 IN ('出库','手持出库') THEN '出库' " +
        "WHEN 操作大类 IN ('入库','手持入库') THEN '入库' " +
        "WHEN 操作大类 IN ('在库','手持在库') THEN '在库' " +
        "WHEN 操作大类 = '退货' THEN '退货' " +
        "WHEN 操作大类 IS NULL THEN " +
        "CASE " +
        "WHEN 单据类型 LIKE '%入库%' AND 单据类型 NOT LIKE '%退货%' THEN '入库' " +
        "WHEN 单据类型 LIKE '%退货%' THEN '退货' " +
        "WHEN 单据类型 LIKE '%转储%' OR 单据类型 LIKE '%加工%' OR 单据类型 LIKE '%库存%' OR 单据类型 LIKE '%拆包%' THEN '在库' " +
        "ELSE '出库' " +
        "END " +
        "ELSE '其他' " +
        "END";

    public String getLatestMonth() {
        QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();
        qw.select("MAX(DATE_FORMAT(创建时间, '%Y-%m')) as latestMonth");
        List<Map<String, Object>> rows = outboundOrderMapper.selectMaps(qw);
        if (!rows.isEmpty() && rows.get(0) != null && rows.get(0).get("latestMonth") != null) {
            return rows.get(0).get("latestMonth").toString();
        }
        // fallback to current month
        java.time.LocalDate now = java.time.LocalDate.now();
        return String.format("%d-%02d", now.getYear(), now.getMonthValue());
    }

    public List<MonthlyBaselineVO> getMonthlyBaseline(String warehouseCode, String startMonth, String endMonth) {
        List<WarehouseVO> warehouses;
        if (warehouseCode != null && !warehouseCode.isEmpty()) {
            WarehouseVO wh = new WarehouseVO();
            wh.setWarehouseCode(warehouseCode);
            wh.setWarehouseName(warehouseService.getWarehouseName(warehouseCode));
            warehouses = List.of(wh);
        } else {
            warehouses = warehouseService.list();
        }
        List<MonthlyBaselineVO> results = new ArrayList<>();
        for (WarehouseVO wh : warehouses) {
            results.addAll(buildMonthlyBaselinesForWarehouse(wh.getWarehouseCode(), wh.getWarehouseName(), startMonth, endMonth, true));
        }
        results.sort(Comparator
                .comparing((MonthlyBaselineVO vo) -> String.format("%d-%02d", vo.getYear(), vo.getMonth())).reversed()
                .thenComparing(MonthlyBaselineVO::getWarehouseCode));
        return results;
    }

    public WarehouseDetailVO getWarehouseDetail(String warehouseCode, Integer year, Integer month) {
        String monthStr = String.format("%d-%02d", year, month);
        String warehouseName = warehouseService.getWarehouseName(warehouseCode);
        if (warehouseName == null) {
            return null;
        }

        MonthlyBaselineVO baseline = buildMonthlyBaseline(warehouseCode, warehouseName, year, month, monthStr, true);
        if (baseline == null) {
            return null;
        }

        WarehouseDetailVO vo = new WarehouseDetailVO();
        vo.setWarehouseCode(warehouseCode);
        vo.setWarehouseName(warehouseName);
        vo.setYear(year);
        vo.setMonth(month);
        vo.setTotalFee(baseline.getTotalFee());
        vo.setDailyAvgFee(baseline.getDailyAvgFee());
        vo.setTotalOrders(baseline.getTotalOrders());
        vo.setTotalItems(baseline.getTotalItems());
        vo.setCostPerOrder(baseline.getCostPerOrder());
        vo.setCostPerItem(baseline.getCostPerItem());
        vo.setAvgHeadcount(baseline.getAvgHeadcount());
        vo.setTotalWorkHours(baseline.getTotalWorkHours());
        vo.setWeightedUnitPrice(baseline.getWeightedUnitPrice());

        // 件单比
        BigDecimal itemsPerOrder = baseline.getTotalOrders() > 0
                ? BigDecimal.valueOf(baseline.getTotalItems())
                    .divide(BigDecimal.valueOf(baseline.getTotalOrders()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setItemsPerOrder(itemsPerOrder);

        // 日均出库单量
        int workDays = getWorkDays(warehouseName, monthStr);
        vo.setDailyAvgOrders(workDays > 0
                ? BigDecimal.valueOf(baseline.getTotalOrders()).divide(BigDecimal.valueOf(workDays), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 各操作类型工时占比（从工作量统计表）
        vo.setWorkHoursBreakdown(getWorkHoursBreakdown(warehouseCode, monthStr));

        // 劳务人员分布
        vo.setLaborDistribution(getLaborDistribution(warehouseName, monthStr));

        return vo;
    }

    public List<CompareResultVO> compare(String codes, String startMonth, String endMonth, String granularity) {
        if (!"month".equalsIgnoreCase(granularity)) {
            return compareByDay(codes, startMonth, endMonth);
        }

        YearMonth start = YearMonth.parse(startMonth);
        YearMonth end = YearMonth.parse(endMonth);
        String[] codeArr = codes.split(",");
        List<CompareResultVO> results = new ArrayList<>();
        for (String code : codeArr) {
            String trimmed = code.trim();
            String whName = warehouseService.getWarehouseName(trimmed);
            List<MonthlyBaselineVO> monthlyBaselines = buildMonthlyBaselinesForWarehouse(trimmed, whName, startMonth, endMonth, false);
            for (MonthlyBaselineVO baseline : monthlyBaselines) {
                String monthStr = String.format("%d-%02d", baseline.getYear(), baseline.getMonth());
                YearMonth ym = YearMonth.parse(monthStr);
                if (ym.isBefore(start) || ym.isAfter(end)) {
                    continue;
                }

                CompareResultVO vo = new CompareResultVO();
                vo.setDate(monthStr + "-01");
                vo.setYear(baseline.getYear());
                vo.setMonth(baseline.getMonth());
                vo.setWarehouseCode(trimmed);
                vo.setWarehouseName(whName);
                vo.setTotalFee(baseline.getTotalFee());
                vo.setTotalOrders(baseline.getTotalOrders());
                vo.setAvgHeadcount(baseline.getAvgHeadcount());
                vo.setDailyAvgFee(baseline.getDailyAvgFee());
                vo.setCostPerOrder(baseline.getCostPerOrder());
                vo.setCostPerItem(baseline.getCostPerItem());
                vo.setTotalWorkHours(baseline.getTotalWorkHours());

                BigDecimal laborEfficiency = BigDecimal.ZERO;
                if (baseline.getAvgHeadcount() > 0 && baseline.getDailyAvgOrders() != null) {
                    laborEfficiency = baseline.getDailyAvgOrders()
                            .divide(BigDecimal.valueOf(baseline.getAvgHeadcount()), 2, RoundingMode.HALF_UP);
                }
                vo.setLaborEfficiency(laborEfficiency);
                results.add(vo);
            }
        }
        results.sort(Comparator
                .comparing(CompareResultVO::getDate)
                .thenComparing(CompareResultVO::getWarehouseCode));
        return results;
    }

    private List<CompareResultVO> compareByDay(String codes, String startMonth, String endMonth) {
        String[] codeArr = codes.split(",");
        List<CompareResultVO> results = new ArrayList<>();
        for (String code : codeArr) {
            String trimmed = code.trim();
            String whName = warehouseService.getWarehouseName(trimmed);
            for (DailyDetailVO daily : getDailyDetail(trimmed, startMonth, endMonth)) {
                LocalDate day = LocalDate.parse(daily.getDate());
                BigDecimal costPerOrder = daily.getObOrders() > 0
                        ? daily.getDailyFee().divide(BigDecimal.valueOf(daily.getObOrders()), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal costPerItem = daily.getObItems() > 0
                        ? daily.getDailyFee().divide(BigDecimal.valueOf(daily.getObItems()), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal laborEfficiency = daily.getHeadcount() > 0
                        ? BigDecimal.valueOf(daily.getObOrders()).divide(BigDecimal.valueOf(daily.getHeadcount()), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                CompareResultVO vo = new CompareResultVO();
                vo.setDate(daily.getDate());
                vo.setYear(day.getYear());
                vo.setMonth(day.getMonthValue());
                vo.setWarehouseCode(trimmed);
                vo.setWarehouseName(whName);
                vo.setTotalFee(daily.getDailyFee());
                vo.setTotalOrders(daily.getObOrders());
                vo.setAvgHeadcount(daily.getHeadcount());
                vo.setDailyAvgFee(daily.getDailyFee());
                vo.setCostPerOrder(costPerOrder);
                vo.setCostPerItem(costPerItem);
                vo.setLaborEfficiency(laborEfficiency);
                vo.setTotalWorkHours(daily.getWorkHours());
                results.add(vo);
            }
        }
        results.sort(Comparator
                .comparing(CompareResultVO::getDate)
                .thenComparing(CompareResultVO::getWarehouseCode));
        return results;
    }

    public List<DailyDetailVO> getDailyDetail(String warehouseCode, String startMonth, String endMonth) {
        List<WarehouseVO> warehouses;
        if (warehouseCode != null && !warehouseCode.isEmpty()) {
            WarehouseVO wh = new WarehouseVO();
            wh.setWarehouseCode(warehouseCode);
            wh.setWarehouseName(warehouseService.getWarehouseName(warehouseCode));
            warehouses = List.of(wh);
        } else {
            warehouses = warehouseService.list();
        }
        List<DailyDetailVO> result = new ArrayList<>();
        for (WarehouseVO wh : warehouses) {
            String whCode = wh.getWarehouseCode();
            String whName = wh.getWarehouseName();
            String attName = warehouseService.getAttendanceWarehouseName(whName);
            BigDecimal unitPrice = dashboardService.getWeightedUnitPrice(whName);

            QueryWrapper<OutboundOrder> orderQw = new QueryWrapper<>();
            orderQw.select("DATE_FORMAT(创建时间, '%Y-%m-%d') as ob_date",
                    "COUNT(*) as ob_orders",
                    "COALESCE(SUM(CAST(物料总数量 AS DECIMAL(20,2))), 0) as ob_items")
                    .eq("库房编码", whCode)
                    .apply("DATE_FORMAT(创建时间, '%Y-%m') BETWEEN {0} AND {1}", startMonth, endMonth)
                    .groupBy("DATE_FORMAT(创建时间, '%Y-%m-%d')");
            Map<String, long[]> orderByDate = new LinkedHashMap<>();
            for (Map<String, Object> row : outboundOrderMapper.selectMaps(orderQw)) {
                String date = String.valueOf(row.get("ob_date"));
                orderByDate.put(date, new long[]{toLong(row.get("ob_orders")), toLong(row.get("ob_items"))});
            }

            QueryWrapper<AttendanceStatistics> attQw = new QueryWrapper<>();
            attQw.select("DATE_FORMAT(考勤日期, '%Y-%m-%d') as att_date",
                    "COUNT(DISTINCT 员工编码) as headcount",
                    "SUM(CAST(工作时长 AS DECIMAL(10,2))) as total_minutes")
                    .apply("DATE_FORMAT(考勤日期, '%Y-%m') BETWEEN {0} AND {1}", startMonth, endMonth)
                    .groupBy("DATE_FORMAT(考勤日期, '%Y-%m-%d')");
            if (attName != null) attQw.eq("库房", attName);
            Map<String, Object[]> attByDate = new LinkedHashMap<>();
            for (Map<String, Object> row : attendanceStatisticsMapper.selectMaps(attQw)) {
                String date = String.valueOf(row.get("att_date"));
                BigDecimal workHours = toBigDecimal(row.get("total_minutes"))
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                attByDate.put(date, new Object[]{toInt(row.get("headcount")), workHours});
            }

            Set<String> allDates = new TreeSet<>();
            allDates.addAll(orderByDate.keySet());
            allDates.addAll(attByDate.keySet());
            for (String date : allDates) {
                long[] orders = orderByDate.getOrDefault(date, new long[]{0L, 0L});
                Object[] att = attByDate.getOrDefault(date, new Object[]{0, BigDecimal.ZERO});
                long obOrders = orders[0];
                long obItems = orders[1];
                int headcount = (Integer) att[0];
                BigDecimal workHours = (BigDecimal) att[1];
                BigDecimal itemOrderRatio = obOrders > 0
                        ? BigDecimal.valueOf(obItems).divide(BigDecimal.valueOf(obOrders), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal dailyFee = workHours.multiply(unitPrice)
                        .multiply(BigDecimal.ONE.add(TAX_RATE)).setScale(2, RoundingMode.HALF_UP);
                DailyDetailVO vo = new DailyDetailVO();
                vo.setDate(date);
                vo.setWarehouseCode(whCode);
                vo.setWarehouseName(whName);
                vo.setObOrders(obOrders);
                vo.setObItems(obItems);
                vo.setItemOrderRatio(itemOrderRatio);
                vo.setHeadcount(headcount);
                vo.setWorkHours(workHours);
                vo.setDailyFee(dailyFee);
                result.add(vo);
            }
        }
        result.sort(Comparator.comparing(DailyDetailVO::getDate).reversed());
        return result;
    }

    public List<OperationFeeDetailVO> getOperationFeeDetail(String warehouseCode, String startMonth, String endMonth, String dimension) {
        if ("day".equalsIgnoreCase(dimension)) {
            return getOperationFeeDetailByDay(warehouseCode, startMonth, endMonth);
        }
        return getOperationFeeDetailByMonth(warehouseCode, startMonth, endMonth);
    }

    private List<OperationFeeDetailVO> getOperationFeeDetailByMonth(String warehouseCode, String startMonth, String endMonth) {
        List<WarehouseVO> warehouses;
        if (warehouseCode != null && !warehouseCode.isEmpty()) {
            WarehouseVO wh = new WarehouseVO();
            wh.setWarehouseCode(warehouseCode);
            wh.setWarehouseName(warehouseService.getWarehouseName(warehouseCode));
            warehouses = List.of(wh);
        } else {
            warehouses = warehouseService.list();
        }

        List<OperationFeeDetailVO> result = new ArrayList<>();
        for (WarehouseVO wh : warehouses) {
            String whCode = wh.getWarehouseCode();
            String whName = wh.getWarehouseName();
            String attWarehouseName = warehouseService.getAttendanceWarehouseName(whName);
            BigDecimal weightedUnitPrice = dashboardService.getWeightedUnitPrice(whName);

            Map<String, BigDecimal> monthFeeMap = new LinkedHashMap<>();
            QueryWrapper<AttendanceStatistics> feeQw = new QueryWrapper<>();
            feeQw.select("DATE_FORMAT(考勤日期, '%Y-%m') as ym",
                "SUM(CAST(工作时长 AS DECIMAL(20,2))) as total_minutes")
                .apply("DATE_FORMAT(考勤日期, '%Y-%m') BETWEEN {0} AND {1}", startMonth, endMonth)
                .groupBy("DATE_FORMAT(考勤日期, '%Y-%m')");
            if (attWarehouseName != null) {
            feeQw.eq("库房", attWarehouseName);
            }
            for (Map<String, Object> row : attendanceStatisticsMapper.selectMaps(feeQw)) {
            String monthLabel = String.valueOf(row.get("ym"));
            BigDecimal workHours = toBigDecimal(row.get("total_minutes"))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            BigDecimal totalFee = workHours
                .multiply(weightedUnitPrice)
                .multiply(BigDecimal.ONE.add(TAX_RATE))
                .setScale(2, RoundingMode.HALF_UP);
            monthFeeMap.put(monthLabel, totalFee);
            }

            // 查找仓库对应的所有工厂编码（工作量表使用工厂编码，与库房编码不同）
            List<String> factoryCodes = warehouseService.getFactoryCodesForWarehouse(whCode);
            if (factoryCodes.isEmpty()) {
                // 无工厂编码映射，跳过工作量查询
                for (String monthLabel : monthFeeMap.keySet()) {
                    BigDecimal totalFee = monthFeeMap.get(monthLabel);
                    if (totalFee.compareTo(BigDecimal.ZERO) <= 0) continue;
                    YearMonth ym = YearMonth.parse(monthLabel, DateTimeFormatter.ofPattern("yyyy-MM"));
                    OperationFeeDetailVO vo = new OperationFeeDetailVO();
                    vo.setDimension("month");
                    vo.setYear(ym.getYear());
                    vo.setMonth(ym.getMonthValue());
                    vo.setMonthLabel(monthLabel);
                    vo.setWarehouseCode(whCode);
                    vo.setWarehouseName(whName);
                    vo.setOperationCategory("未分类");
                    vo.setOperationCount(0L);
                    vo.setFeeRatio(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
                    vo.setEstimatedFee(totalFee.setScale(2, RoundingMode.HALF_UP));
                    result.add(vo);
                }
                continue;
            }

            QueryWrapper<WorkloadStatisticsDetail> qw = new QueryWrapper<>();
            qw.select("DATE_FORMAT(操作时间, '%Y-%m') as ym",
                    OP_CATEGORY_CASE + " as op_category",
                    "COUNT(*) as op_cnt")
                    .in("工厂编码", factoryCodes)
                    .apply("DATE_FORMAT(操作时间, '%Y-%m') BETWEEN {0} AND {1}", startMonth, endMonth)
                    .groupBy("DATE_FORMAT(操作时间, '%Y-%m')", OP_CATEGORY_CASE);

            List<Map<String, Object>> rows = workloadStatisticsDetailMapper.selectMaps(qw);
            Map<String, List<Map<String, Object>>> groupedByMonth = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String monthLabel = String.valueOf(row.get("ym"));
                groupedByMonth.computeIfAbsent(monthLabel, k -> new ArrayList<>()).add(row);
            }

            Set<String> monthKeys = new TreeSet<>();
            monthKeys.addAll(monthFeeMap.keySet());
            monthKeys.addAll(groupedByMonth.keySet());

            for (String monthLabel : monthKeys) {
                BigDecimal totalFee = monthFeeMap.getOrDefault(monthLabel, BigDecimal.ZERO);
                List<Map<String, Object>> monthRows = groupedByMonth.getOrDefault(monthLabel, List.of());

                long totalOps = 0L;
                for (Map<String, Object> row : monthRows) {
                    totalOps += toLong(row.get("op_cnt"));
                }

                if (totalFee.compareTo(BigDecimal.ZERO) <= 0 && totalOps <= 0) {
                    continue;
                }

                YearMonth ym = YearMonth.parse(monthLabel, DateTimeFormatter.ofPattern("yyyy-MM"));
                if (monthRows.isEmpty()) {
                    OperationFeeDetailVO vo = new OperationFeeDetailVO();
                    vo.setDimension("month");
                    vo.setYear(ym.getYear());
                    vo.setMonth(ym.getMonthValue());
                    vo.setMonthLabel(monthLabel);
                    vo.setWarehouseCode(whCode);
                    vo.setWarehouseName(whName);
                    vo.setOperationCategory("未分类");
                    vo.setOperationCount(0L);
                    vo.setFeeRatio(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
                    vo.setEstimatedFee(totalFee.setScale(2, RoundingMode.HALF_UP));
                    result.add(vo);
                    continue;
                }

                for (Map<String, Object> row : monthRows) {
                    long opCnt = toLong(row.get("op_cnt"));
                    BigDecimal ratio = totalOps > 0
                            ? BigDecimal.valueOf(opCnt).divide(BigDecimal.valueOf(totalOps), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal fee = totalFee.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

                    OperationFeeDetailVO vo = new OperationFeeDetailVO();
                    vo.setDimension("month");
                    vo.setYear(ym.getYear());
                    vo.setMonth(ym.getMonthValue());
                    vo.setMonthLabel(monthLabel);
                    vo.setWarehouseCode(whCode);
                    vo.setWarehouseName(whName);
                    vo.setOperationCategory(String.valueOf(row.get("op_category")));
                    vo.setOperationCount(opCnt);
                    vo.setFeeRatio(ratio);
                    vo.setEstimatedFee(fee);
                    result.add(vo);
                }
            }
        }

        result.sort(Comparator
                .comparing(OperationFeeDetailVO::getMonthLabel).reversed()
                .thenComparing(OperationFeeDetailVO::getEstimatedFee, Comparator.reverseOrder())
                .thenComparing(OperationFeeDetailVO::getWarehouseCode));
        return result;
    }

    private List<OperationFeeDetailVO> getOperationFeeDetailByDay(String warehouseCode, String startMonth, String endMonth) {
        List<WarehouseVO> warehouses;
        if (warehouseCode != null && !warehouseCode.isEmpty()) {
            WarehouseVO wh = new WarehouseVO();
            wh.setWarehouseCode(warehouseCode);
            wh.setWarehouseName(warehouseService.getWarehouseName(warehouseCode));
            warehouses = List.of(wh);
        } else {
            warehouses = warehouseService.list();
        }

        List<OperationFeeDetailVO> result = new ArrayList<>();
        for (WarehouseVO wh : warehouses) {
            String whCode = wh.getWarehouseCode();
            String whName = wh.getWarehouseName();

            Map<String, BigDecimal> dayFeeMap = new LinkedHashMap<>();
            for (DailyDetailVO daily : getDailyDetail(whCode, startMonth, endMonth)) {
                dayFeeMap.put(daily.getDate(), daily.getDailyFee());
            }

            // 查找仓库对应的所有工厂编码
            List<String> factoryCodes = warehouseService.getFactoryCodesForWarehouse(whCode);
            if (factoryCodes.isEmpty()) {
                for (String day : dayFeeMap.keySet()) {
                    BigDecimal totalFee = dayFeeMap.get(day);
                    if (totalFee.compareTo(BigDecimal.ZERO) <= 0) continue;
                    LocalDate dt = LocalDate.parse(day);
                    OperationFeeDetailVO vo = new OperationFeeDetailVO();
                    vo.setDimension("day");
                    vo.setDate(day);
                    vo.setYear(dt.getYear());
                    vo.setMonth(dt.getMonthValue());
                    vo.setMonthLabel(String.format("%d-%02d", dt.getYear(), dt.getMonthValue()));
                    vo.setWarehouseCode(whCode);
                    vo.setWarehouseName(whName);
                    vo.setOperationCategory("未分类");
                    vo.setOperationCount(0L);
                    vo.setFeeRatio(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
                    vo.setEstimatedFee(totalFee.setScale(2, RoundingMode.HALF_UP));
                    result.add(vo);
                }
                continue;
            }

            QueryWrapper<WorkloadStatisticsDetail> qw = new QueryWrapper<>();
            qw.select("DATE_FORMAT(操作时间, '%Y-%m-%d') as dt",
                    OP_CATEGORY_CASE + " as op_category",
                    "COUNT(*) as op_cnt")
                    .in("工厂编码", factoryCodes)
                    .apply("DATE_FORMAT(操作时间, '%Y-%m') BETWEEN {0} AND {1}", startMonth, endMonth)
                    .groupBy("DATE_FORMAT(操作时间, '%Y-%m-%d')", OP_CATEGORY_CASE);

            List<Map<String, Object>> rows = workloadStatisticsDetailMapper.selectMaps(qw);
            Map<String, List<Map<String, Object>>> groupedByDay = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String day = String.valueOf(row.get("dt"));
                groupedByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(row);
            }

            Set<String> dayKeys = new TreeSet<>();
            dayKeys.addAll(dayFeeMap.keySet());
            dayKeys.addAll(groupedByDay.keySet());

            for (String day : dayKeys) {
                BigDecimal totalFee = dayFeeMap.getOrDefault(day, BigDecimal.ZERO);
                List<Map<String, Object>> dayRows = groupedByDay.getOrDefault(day, List.of());
                long totalOps = 0L;
                for (Map<String, Object> row : dayRows) {
                    totalOps += toLong(row.get("op_cnt"));
                }

                if (totalFee.compareTo(BigDecimal.ZERO) <= 0 && totalOps <= 0) {
                    continue;
                }

                LocalDate dt = LocalDate.parse(day);
                if (dayRows.isEmpty()) {
                    OperationFeeDetailVO vo = new OperationFeeDetailVO();
                    vo.setDimension("day");
                    vo.setDate(day);
                    vo.setYear(dt.getYear());
                    vo.setMonth(dt.getMonthValue());
                    vo.setMonthLabel(String.format("%d-%02d", dt.getYear(), dt.getMonthValue()));
                    vo.setWarehouseCode(whCode);
                    vo.setWarehouseName(whName);
                    vo.setOperationCategory("未分类");
                    vo.setOperationCount(0L);
                    vo.setFeeRatio(BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP));
                    vo.setEstimatedFee(totalFee.setScale(2, RoundingMode.HALF_UP));
                    result.add(vo);
                    continue;
                }

                for (Map<String, Object> row : dayRows) {
                    long opCnt = toLong(row.get("op_cnt"));
                    BigDecimal ratio = totalOps > 0
                            ? BigDecimal.valueOf(opCnt).divide(BigDecimal.valueOf(totalOps), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal fee = totalFee.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

                    OperationFeeDetailVO vo = new OperationFeeDetailVO();
                    vo.setDimension("day");
                    vo.setDate(day);
                    vo.setYear(dt.getYear());
                    vo.setMonth(dt.getMonthValue());
                    vo.setMonthLabel(String.format("%d-%02d", dt.getYear(), dt.getMonthValue()));
                    vo.setWarehouseCode(whCode);
                    vo.setWarehouseName(whName);
                    vo.setOperationCategory(String.valueOf(row.get("op_category")));
                    vo.setOperationCount(opCnt);
                    vo.setFeeRatio(ratio);
                    vo.setEstimatedFee(fee);
                    result.add(vo);
                }
            }
        }

        result.sort(Comparator
                .comparing(OperationFeeDetailVO::getDate).reversed()
                .thenComparing(OperationFeeDetailVO::getEstimatedFee, Comparator.reverseOrder())
                .thenComparing(OperationFeeDetailVO::getWarehouseCode));
        return result;
    }

    // --- 内部方法 ---

        private List<MonthlyBaselineVO> buildMonthlyBaselinesForWarehouse(
            String warehouseCode,
            String warehouseName,
            String startMonth,
            String endMonth,
            boolean includeEnrichment) {
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        BigDecimal weightedUnitPrice = dashboardService.getWeightedUnitPrice(warehouseName);

        QueryWrapper<OutboundOrder> orderQw = new QueryWrapper<>();
        orderQw.select("DATE_FORMAT(创建时间, '%Y-%m') as ym",
            "COUNT(*) as totalOrders",
            "SUM(CAST(物料总数量 AS DECIMAL(20,2))) as totalItems")
            .eq("库房编码", warehouseCode)
            .apply("DATE_FORMAT(创建时间, '%Y-%m') BETWEEN {0} AND {1}", startMonth, endMonth)
            .groupBy("DATE_FORMAT(创建时间, '%Y-%m')");
        Map<String, long[]> orderByMonth = new LinkedHashMap<>();
        for (Map<String, Object> row : outboundOrderMapper.selectMaps(orderQw)) {
            String ym = String.valueOf(row.get("ym"));
            orderByMonth.put(ym, new long[]{toLong(row.get("totalOrders")), toLong(row.get("totalItems"))});
        }

        QueryWrapper<AttendanceStatistics> attQw = new QueryWrapper<>();
        attQw.select("DATE_FORMAT(考勤日期, '%Y-%m') as ym",
            "SUM(CAST(工作时长 AS DECIMAL(20,2))) as totalWorkHours",
            "COUNT(DISTINCT 员工编码) as distinctEmployees",
            "COUNT(DISTINCT 考勤日期) as workDays")
            .apply("DATE_FORMAT(考勤日期, '%Y-%m') BETWEEN {0} AND {1}", startMonth, endMonth)
            .groupBy("DATE_FORMAT(考勤日期, '%Y-%m')");
        if (attWarehouseName != null) {
            attQw.eq("库房", attWarehouseName);
        }
        Map<String, Object[]> attByMonth = new LinkedHashMap<>();
        for (Map<String, Object> row : attendanceStatisticsMapper.selectMaps(attQw)) {
            String ym = String.valueOf(row.get("ym"));
            BigDecimal totalWorkHours = toBigDecimal(row.get("totalWorkHours"));
            int avgHeadcount = toInt(row.get("distinctEmployees"));
            int workDays = Math.max(1, toInt(row.get("workDays")));
            attByMonth.put(ym, new Object[]{totalWorkHours, avgHeadcount, workDays});
        }

        Set<String> monthKeys = new TreeSet<>();
        monthKeys.addAll(orderByMonth.keySet());
        monthKeys.addAll(attByMonth.keySet());

        List<MonthlyBaselineVO> result = new ArrayList<>();
        for (String monthStr : monthKeys) {
            long[] orderInfo = orderByMonth.getOrDefault(monthStr, new long[]{0L, 0L});
            Object[] attInfo = attByMonth.getOrDefault(monthStr, new Object[]{BigDecimal.ZERO, 0, 1});

            long totalOrders = orderInfo[0];
            long totalItems = orderInfo[1];
            BigDecimal totalWorkHours = (BigDecimal) attInfo[0];
            int avgHeadcount = (Integer) attInfo[1];
            int workDays = (Integer) attInfo[2];

            if (totalOrders == 0 && totalItems == 0 && totalWorkHours.compareTo(BigDecimal.ZERO) <= 0) {
            continue;
            }

            YearMonth ym = YearMonth.parse(monthStr);
            BigDecimal totalFee = totalWorkHours
                .multiply(weightedUnitPrice)
                .multiply(BigDecimal.ONE.add(TAX_RATE))
                .setScale(2, RoundingMode.HALF_UP);
            BigDecimal dailyAvgFee = totalFee.divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP);
            BigDecimal costPerOrder = totalOrders > 0
                ? totalFee.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            BigDecimal costPerItem = totalItems > 0
                ? totalFee.divide(BigDecimal.valueOf(totalItems), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            BigDecimal itemsPerOrder = totalOrders > 0
                ? BigDecimal.valueOf(totalItems).divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            BigDecimal dailyAvgOrders = BigDecimal.valueOf(totalOrders).divide(BigDecimal.valueOf(workDays), 1, RoundingMode.HALF_UP);
            BigDecimal laborEff = avgHeadcount > 0
                ? dailyAvgOrders.divide(BigDecimal.valueOf(avgHeadcount), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            MonthlyBaselineVO vo = new MonthlyBaselineVO();
            vo.setWarehouseCode(warehouseCode);
            vo.setWarehouseName(warehouseName);
            vo.setYear(ym.getYear());
            vo.setMonth(ym.getMonthValue());
            vo.setDailyAvgFee(dailyAvgFee);
            vo.setTotalFee(totalFee);
            vo.setTotalOrders(totalOrders);
            vo.setTotalItems(totalItems);
            vo.setCostPerOrder(costPerOrder);
            vo.setCostPerItem(costPerItem);
            vo.setAvgHeadcount(avgHeadcount);
            vo.setTotalWorkHours(totalWorkHours);
            vo.setWeightedUnitPrice(weightedUnitPrice);
            vo.setItemsPerOrder(itemsPerOrder);
            vo.setDailyAvgOrders(dailyAvgOrders);
            vo.setLaborEfficiency(laborEff);

            if (includeEnrichment) {
            Map<String, Object> laborDist = getLaborDistribution(warehouseName, monthStr);
            int fixedCount = (int) laborDist.getOrDefault("fixedCount", 0);
            int tempCount = (int) laborDist.getOrDefault("tempCount", 0);
            vo.setFixedTempRatio(tempCount > 0
                ? BigDecimal.valueOf(fixedCount).divide(BigDecimal.valueOf(tempCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
            vo.setWarehouseType(classifyWarehouse(totalOrders, itemsPerOrder, vo.getFixedTempRatio()));
            } else {
            vo.setFixedTempRatio(BigDecimal.ZERO);
            vo.setWarehouseType(null);
            }

            result.add(vo);
        }

        result.sort(Comparator
            .comparing((MonthlyBaselineVO vo) -> String.format("%d-%02d", vo.getYear(), vo.getMonth())).reversed());
        return result;
        }

    private MonthlyBaselineVO buildMonthlyBaseline(String warehouseCode, String warehouseName,
                                                    Integer year, Integer month, String monthStr,
                                                    boolean includeEnrichment) {
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        // 出库单聚合
        QueryWrapper<OutboundOrder> orderQw = new QueryWrapper<>();
        orderQw.select("COUNT(*) as totalOrders",
                "SUM(CAST(物料总数量 AS DECIMAL(20,2))) as totalItems");
        orderQw.eq("库房编码", warehouseCode);
        orderQw.apply("DATE_FORMAT(创建时间, '%Y-%m') = {0}", monthStr);
        List<Map<String, Object>> orderRows = outboundOrderMapper.selectMaps(orderQw);

        long totalOrders = 0;
        long totalItems = 0;
        if (!orderRows.isEmpty() && orderRows.get(0) != null) {
            totalOrders = toLong(orderRows.get(0).get("totalOrders"));
            totalItems = toLong(orderRows.get(0).get("totalItems"));
        }

        // 出勤统计聚合
        QueryWrapper<AttendanceStatistics> attQw = new QueryWrapper<>();
        attQw.select("SUM(CAST(工作时长 AS DECIMAL(10,2))) as totalWorkHours",
                "COUNT(DISTINCT 员工编码) as distinctEmployees",
                "COUNT(DISTINCT 考勤日期) as workDays");
        if (attWarehouseName != null) {
            attQw.eq("库房", attWarehouseName);
        }
        attQw.apply("DATE_FORMAT(考勤日期, '%Y-%m') = {0}", monthStr);
        List<Map<String, Object>> attRows = attendanceStatisticsMapper.selectMaps(attQw);

        BigDecimal totalWorkHours = BigDecimal.ZERO;
        int workDays = 1;
        int avgHeadcount = 0;
        if (!attRows.isEmpty() && attRows.get(0) != null) {
            Map<String, Object> att = attRows.get(0);
            totalWorkHours = toBigDecimal(att.get("totalWorkHours"));
            workDays = Math.max(1, toInt(att.get("workDays")));
            avgHeadcount = toInt(att.get("distinctEmployees"));
        }

        // 加权平均单价
        BigDecimal weightedUnitPrice = dashboardService.getWeightedUnitPrice(warehouseName);

        // 费用计算
        BigDecimal totalFee = totalWorkHours
                .multiply(weightedUnitPrice)
                .multiply(BigDecimal.ONE.add(TAX_RATE))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal dailyAvgFee = totalFee.divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP);
        BigDecimal costPerOrder = totalOrders > 0
                ? totalFee.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal costPerItem = totalItems > 0
                ? totalFee.divide(BigDecimal.valueOf(totalItems), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        if (totalOrders == 0 && totalItems == 0 && totalWorkHours.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        MonthlyBaselineVO vo = new MonthlyBaselineVO();
        vo.setWarehouseCode(warehouseCode);
        vo.setWarehouseName(warehouseName);
        vo.setYear(year);
        vo.setMonth(month);
        vo.setDailyAvgFee(dailyAvgFee);
        vo.setTotalFee(totalFee);
        vo.setTotalOrders(totalOrders);
        vo.setTotalItems(totalItems);
        vo.setCostPerOrder(costPerOrder);
        vo.setCostPerItem(costPerItem);
        vo.setAvgHeadcount(avgHeadcount);
        vo.setTotalWorkHours(totalWorkHours);
        vo.setWeightedUnitPrice(weightedUnitPrice);

        // 新增指标
        BigDecimal itemsPerOrder = totalOrders > 0
                ? BigDecimal.valueOf(totalItems).divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setItemsPerOrder(itemsPerOrder);
        vo.setDailyAvgOrders(workDays > 0
                ? BigDecimal.valueOf(totalOrders).divide(BigDecimal.valueOf(workDays), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        BigDecimal laborEff = (avgHeadcount > 0 && workDays > 0)
                ? BigDecimal.valueOf(totalOrders).divide(BigDecimal.valueOf((long) avgHeadcount * workDays), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setLaborEfficiency(laborEff);

        if (includeEnrichment) {
            Map<String, Object> laborDist = getLaborDistribution(warehouseName, monthStr);
            int fixedCount = (int) laborDist.getOrDefault("fixedCount", 0);
            int tempCount = (int) laborDist.getOrDefault("tempCount", 0);
            vo.setFixedTempRatio(tempCount > 0
                    ? BigDecimal.valueOf(fixedCount).divide(BigDecimal.valueOf(tempCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            vo.setWarehouseType(classifyWarehouse(totalOrders, itemsPerOrder, vo.getFixedTempRatio()));
        } else {
            vo.setFixedTempRatio(BigDecimal.ZERO);
            vo.setWarehouseType(null);
        }
        return vo;
    }

    private int getWorkDays(String warehouseName, String monthStr) {
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        QueryWrapper<AttendanceStatistics> qw = new QueryWrapper<>();
        qw.select("COUNT(DISTINCT 考勤日期) as workDays");
        if (attWarehouseName != null) {
            qw.eq("库房", attWarehouseName);
        }
        qw.apply("DATE_FORMAT(考勤日期, '%Y-%m') = {0}", monthStr);
        List<Map<String, Object>> rows = attendanceStatisticsMapper.selectMaps(qw);
        if (rows.isEmpty() || rows.get(0) == null) return 1;
        return Math.max(1, toInt(rows.get(0).get("workDays")));
    }

    private Map<String, BigDecimal> getWorkHoursBreakdown(String warehouseCode, String monthStr) {
        // 从工作量统计表按操作大类聚合（使用统一分类逻辑，通过工厂编码映射）
        List<String> factoryCodes = warehouseService.getFactoryCodesForWarehouse(warehouseCode);
        if (factoryCodes.isEmpty()) {
            return new LinkedHashMap<>();
        }
        QueryWrapper<WorkloadStatisticsInfo> qw = new QueryWrapper<>();
        qw.select(OP_CATEGORY_CASE + " as category", "COUNT(*) as cnt")
                .in("工厂编码", factoryCodes)
                .groupBy(OP_CATEGORY_CASE);
        List<Map<String, Object>> rows = workloadStatisticsInfoMapper.selectMaps(qw);

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String category = String.valueOf(row.get("category"));
            BigDecimal cnt = toBigDecimal(row.get("cnt"));
            breakdown.put(category, cnt);
        }
        return breakdown;
    }

    private Map<String, Object> getLaborDistribution(String warehouseName, String monthStr) {
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        QueryWrapper<AttendanceStatistics> qw = new QueryWrapper<>();
        qw.select("员工类型 as empType", "COUNT(DISTINCT 员工编码) as cnt");
        if (attWarehouseName != null) {
            qw.eq("库房", attWarehouseName);
        }
        qw.apply("DATE_FORMAT(考勤日期, '%Y-%m') = {0}", monthStr)
                .groupBy("员工类型");
        List<Map<String, Object>> rows = attendanceStatisticsMapper.selectMaps(qw);

        int fixedCount = 0;
        int tempCount = 0;
        for (Map<String, Object> row : rows) {
            String empType = String.valueOf(row.get("empType"));
            int cnt = toInt(row.get("cnt"));
            // 自有人员 + 长期劳务 = 固定人员；临时劳务 = 临时人员
            if ("自有人员".equals(empType) || "长期劳务".equals(empType)) {
                fixedCount += cnt;
            } else if ("临时劳务".equals(empType)) {
                tempCount += cnt;
            }
        }
        int total = fixedCount + tempCount;
        Map<String, Object> dist = new LinkedHashMap<>();
        dist.put("fixedCount", fixedCount);
        dist.put("tempCount", tempCount);
        dist.put("fixedRatio", total > 0 ? BigDecimal.valueOf(fixedCount).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return dist;
    }

    private String classifyWarehouse(long monthlyOrders, BigDecimal itemsPerOrder, BigDecimal fixedTempRatio) {
        // 规模类型
        String scale;
        if (monthlyOrders > 200000) scale = "大型仓";
        else if (monthlyOrders > 100000) scale = "中大型仓";
        else if (monthlyOrders > 50000) scale = "中型仓";
        else scale = "小型仓";

        // 件单比类型
        String itemType;
        double ipr = itemsPerOrder.doubleValue();
        if (ipr < 3) itemType = "低件单比";
        else if (ipr <= 6) itemType = "中件单比";
        else itemType = "高件单比";

        // 劳务结构
        String laborType;
        double ftr = fixedTempRatio.doubleValue();
        if (ftr > 1) laborType = "固临均衡型";
        else if (ftr >= 0.5) laborType = "混合型";
        else laborType = "临时主导型";

        return scale + " / " + itemType + " / " + laborType;
    }
}
