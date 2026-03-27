package com.kejie.whop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kejie.whop.mapper.AttendanceStatisticsMapper;
import com.kejie.whop.mapper.OutboundOrderMapper;
import com.kejie.whop.mapper.QuotationInfoMapper;
import com.kejie.whop.model.dto.EstimateRequest;
import com.kejie.whop.model.entity.AttendanceStatistics;
import com.kejie.whop.model.entity.OutboundOrder;
import com.kejie.whop.model.entity.QuotationInfo;
import com.kejie.whop.model.vo.EstimateResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static com.kejie.whop.service.DashboardService.*;

@Service
@RequiredArgsConstructor
public class EstimateService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final AttendanceStatisticsMapper attendanceStatisticsMapper;
    private final QuotationInfoMapper quotationInfoMapper;
    private final WarehouseService warehouseService;

    private final BaselineService baselineService;

    private static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");
    private static final BigDecimal ITEM_CORRECTION_RATE = new BigDecimal("0.05");

    public EstimateResultVO calculate(EstimateRequest req) {
        BigDecimal dailyOrders = req.getDailyOrders();
        BigDecimal itemsPerOrder = req.getItemsPerOrder();
        int workDays = req.getWorkDays();
        BigDecimal laborEfficiency = req.getLaborEfficiency();
        BigDecimal fixedLaborPrice = req.getFixedLaborPrice();
        BigDecimal tempLaborPrice = req.getTempLaborPrice();
        BigDecimal forkliftLaborPrice = req.getForkliftLaborPrice() != null ? req.getForkliftLaborPrice() : BigDecimal.ZERO;
        BigDecimal fixedLaborRatio = req.getFixedLaborRatio();
        BigDecimal forkliftLaborRatio = req.getForkliftLaborRatio() != null ? req.getForkliftLaborRatio() : BigDecimal.ZERO;
        BigDecimal tempLaborRatio = req.getTempLaborRatio() != null
                ? req.getTempLaborRatio()
                : BigDecimal.ONE.subtract(fixedLaborRatio).subtract(forkliftLaborRatio);
        BigDecimal hoursPerDay = req.getHoursPerDay() != null ? req.getHoursPerDay() : HOURS_PER_DAY;
        BigDecimal taxRate = req.getTaxRate() != null ? req.getTaxRate() : new BigDecimal("0.06");

        // Step 2: 人力需求 = ceil(日均单量 / 人效)
        int baseHeadcount = dailyOrders
                .divide(laborEfficiency, 0, RoundingMode.CEILING).intValue();

        // 件单比修正系数
        BigDecimal itemCorrectionFactor = BigDecimal.ONE;
        if (Boolean.TRUE.equals(req.getEnableItemCorrection()) && req.getItemsPerOrderBaseline() != null) {
            BigDecimal diff = itemsPerOrder.subtract(req.getItemsPerOrderBaseline());
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                itemCorrectionFactor = BigDecimal.ONE.add(ITEM_CORRECTION_RATE.multiply(diff));
            }
        }
        int estimatedHeadcount = BigDecimal.valueOf(baseHeadcount)
                .multiply(itemCorrectionFactor).setScale(0, RoundingMode.CEILING).intValue();

        // Step 3: 月度总工时 = 人数 × 人均日工时 × 工作天数
        BigDecimal estimatedTotalHours = BigDecimal.valueOf(estimatedHeadcount)
                .multiply(hoursPerDay)
                .multiply(BigDecimal.valueOf(workDays));

        // Step 4: 加权单价 = α×固定 + β×临时 + γ×叉车
        BigDecimal weightedUnitPrice = fixedLaborRatio.multiply(fixedLaborPrice)
                .add(tempLaborRatio.multiply(tempLaborPrice))
                .add(forkliftLaborRatio.multiply(forkliftLaborPrice))
                .setScale(2, RoundingMode.HALF_UP);

        // 月度费用 = 总工时 × 加权单价 × (1 + 税率)
        BigDecimal monthlyFee = estimatedTotalHours
                .multiply(weightedUnitPrice)
                .multiply(BigDecimal.ONE.add(taxRate))
                .setScale(2, RoundingMode.HALF_UP);

        // Step 5: 报价输出
        BigDecimal totalOrders = dailyOrders.multiply(BigDecimal.valueOf(workDays));
        BigDecimal costPerOrder = totalOrders.compareTo(BigDecimal.ZERO) > 0
                ? monthlyFee.divide(totalOrders, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal totalItems = dailyOrders.multiply(itemsPerOrder).multiply(BigDecimal.valueOf(workDays));
        BigDecimal costPerItem = totalItems.compareTo(BigDecimal.ZERO) > 0
                ? monthlyFee.divide(totalItems, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal dailyFee = workDays > 0
                ? monthlyFee.divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        EstimateResultVO result = new EstimateResultVO();
        result.setEstimatedHeadcount(estimatedHeadcount);
        result.setEstimatedTotalHours(estimatedTotalHours);
        result.setWeightedUnitPrice(weightedUnitPrice);
        result.setMonthlyFee(monthlyFee);
        result.setDailyFee(dailyFee);
        result.setCostPerOrder(costPerOrder);
        result.setCostPerItem(costPerItem);
        result.setItemCorrectionFactor(itemCorrectionFactor);
        return result;
    }

    public EstimateRequest getDefaults(String warehouseCode) {
        String warehouseName = warehouseService.getWarehouseName(warehouseCode);
        EstimateRequest defaults = new EstimateRequest();

        // 默认值
        defaults.setDailyOrders(new BigDecimal("500"));
        defaults.setItemsPerOrder(new BigDecimal("4.25"));
        defaults.setItemsPerOrderBaseline(new BigDecimal("4.25"));
        defaults.setWorkDays(26);
        defaults.setLaborEfficiency(new BigDecimal("50"));
        defaults.setFixedLaborPrice(new BigDecimal("22.00"));
        defaults.setTempLaborPrice(new BigDecimal("18.00"));
        defaults.setForkliftLaborPrice(new BigDecimal("25.00"));
        defaults.setFixedLaborRatio(new BigDecimal("0.30"));
        defaults.setTempLaborRatio(new BigDecimal("0.60"));
        defaults.setForkliftLaborRatio(new BigDecimal("0.10"));
        defaults.setHoursPerDay(new BigDecimal("8"));
        defaults.setEnableItemCorrection(false);
        defaults.setTaxRate(new BigDecimal("0.06"));

        if (warehouseName == null) {
            return defaults;
        }

        // 从出库单表计算日均单量和件单比
        QueryWrapper<OutboundOrder> orderQw = new QueryWrapper<>();
        orderQw.select("COUNT(*) as totalOrders",
                "SUM(CAST(物料总数量 AS DECIMAL(20,2))) as totalItems",
                "COUNT(DISTINCT DATE(创建时间)) as workDays")
                .eq("库房编码", warehouseCode);
        List<Map<String, Object>> orderRows = outboundOrderMapper.selectMaps(orderQw);

        if (!orderRows.isEmpty() && orderRows.get(0) != null && orderRows.get(0).get("totalOrders") != null) {
            Map<String, Object> row = orderRows.get(0);
            long totalOrders = toLong(row.get("totalOrders"));
            long totalItems = toLong(row.get("totalItems"));
            int workDays = Math.max(1, toInt(row.get("workDays")));

            if (totalOrders > 0) {
                defaults.setDailyOrders(BigDecimal.valueOf(totalOrders)
                        .divide(BigDecimal.valueOf(workDays), 1, RoundingMode.HALF_UP));
                defaults.setItemsPerOrder(BigDecimal.valueOf(totalItems)
                        .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP));
                defaults.setWorkDays(workDays);
            }
        }

        // 从出勤统计表计算人效
        String attWarehouseName = warehouseService.getAttendanceWarehouseName(warehouseName);
        QueryWrapper<AttendanceStatistics> attQw = new QueryWrapper<>();
        attQw.select("COUNT(DISTINCT 员工编码) as distinctEmployees",
                "COUNT(DISTINCT 考勤日期) as attDays")
                .eq("库房", attWarehouseName);
        List<Map<String, Object>> attRows = attendanceStatisticsMapper.selectMaps(attQw);

        if (!attRows.isEmpty() && attRows.get(0) != null && attRows.get(0).get("distinctEmployees") != null) {
            int employees = toInt(attRows.get(0).get("distinctEmployees"));
            int attDays = Math.max(1, toInt(attRows.get(0).get("attDays")));
            if (employees > 0 && defaults.getDailyOrders().compareTo(BigDecimal.ZERO) > 0) {
                defaults.setLaborEfficiency(defaults.getDailyOrders()
                        .divide(BigDecimal.valueOf(employees), 1, RoundingMode.HALF_UP));
            }
        }

        // 从报价信息表按类型获取劳务单价
        QueryWrapper<QuotationInfo> priceQw = new QueryWrapper<>();
        priceQw.select("结费类型 as feeType", "AVG(供应商结算单价) as avgPrice")
                .eq("库房名称", warehouseName)
                .isNotNull("供应商结算单价")
                .groupBy("结费类型");
        List<Map<String, Object>> priceRows = quotationInfoMapper.selectMaps(priceQw);

        for (Map<String, Object> row : priceRows) {
            if (row == null) continue;
            String feeType = String.valueOf(row.get("feeType"));
            BigDecimal avg = toBigDecimal(row.get("avgPrice")).setScale(2, RoundingMode.HALF_UP);
            if (feeType.contains("固定")) {
                defaults.setFixedLaborPrice(avg);
            } else if (feeType.contains("叉车")) {
                defaults.setForkliftLaborPrice(avg);
            } else if (feeType.contains("临时")) {
                defaults.setTempLaborPrice(avg);
            }
        }

        // 从出勤统计表获取劳务类型占比
        String attName = warehouseService.getAttendanceWarehouseName(warehouseName);
        if (attName != null) {
            QueryWrapper<AttendanceStatistics> ratioQw = new QueryWrapper<>();
            ratioQw.select("员工类型 as empType", "COUNT(DISTINCT 员工编码) as cnt")
                    .eq("库房", attName)
                    .groupBy("员工类型");
            List<Map<String, Object>> ratioRows = attendanceStatisticsMapper.selectMaps(ratioQw);
            int fixedCnt = 0, tempCnt = 0, forkliftCnt = 0;
            for (Map<String, Object> row : ratioRows) {
                if (row == null) continue;
                String empType = String.valueOf(row.get("empType"));
                int cnt = toInt(row.get("cnt"));
                // 自有人员 + 长期劳务 = 固定人员；临时劳务 = 临时人员
                if ("自有人员".equals(empType) || "长期劳务".equals(empType)) fixedCnt += cnt;
                else if ("临时劳务".equals(empType)) tempCnt += cnt;
            }
            int total = fixedCnt + tempCnt + forkliftCnt;
            if (total > 0) {
                defaults.setFixedLaborRatio(BigDecimal.valueOf(fixedCnt).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
                defaults.setTempLaborRatio(BigDecimal.valueOf(tempCnt).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
                defaults.setForkliftLaborRatio(BigDecimal.valueOf(forkliftCnt).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            }
        }

        // 件单比基线
        defaults.setItemsPerOrderBaseline(defaults.getItemsPerOrder());

        return defaults;
    }
}
