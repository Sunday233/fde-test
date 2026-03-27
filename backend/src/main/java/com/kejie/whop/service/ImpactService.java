package com.kejie.whop.service;

import com.kejie.whop.client.AnalyticsClient;
import com.kejie.whop.model.vo.CorrelationMatrixVO;
import com.kejie.whop.model.vo.FactorRankVO;
import com.kejie.whop.model.vo.ScatterPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImpactService {

    private final AnalyticsClient analyticsClient;

    private static final Map<String, String> APPLICATION_TIPS = Map.ofEntries(
            Map.entry("出勤人数", "直接决定工时总量，是费用核心驱动。公式：工时 = 人数 × 人均工时"),
            Map.entry("临时劳务人数", "提供弹性产能，单价较高，旺季弹性增加临时劳务"),
            Map.entry("固定劳务人数", "提供稳定产能，单价较低，淡季维持核心班底"),
            Map.entry("出库单量", "驱动拣货/复核/打包全链路工作量，作为人力需求推算基础"),
            Map.entry("出库件数", "件数越多复核/称重时间越长，影响单均操作成本"),
            Map.entry("件单比", "件单比越高单均拣货时间越长，通过修正系数K调整人力需求"),
            Map.entry("入库单量", "入库操作(收货→上架)占用独立人力，叠加出库高峰费用陡增"),
            Map.entry("退货量", "退货质检/上架占用逆向物流人力，降低退货率可减少操作成本"),
            Map.entry("上架量", "入库上架需叉车/人工配合，影响仓储周转效率"),
            Map.entry("固临比", "决定加权平均单价水平，比例越高平均单价越低")
    );

    public List<FactorRankVO> getFactors(String warehouseCode) {
        List<FactorRankVO> factors = analyticsClient.getFactors(warehouseCode);
        for (FactorRankVO f : factors) {
            enrichTierInfo(f);
        }
        return factors;
    }

    public CorrelationMatrixVO getCorrelation(String warehouseCode) {
        return analyticsClient.getCorrelation(warehouseCode);
    }

    public List<ScatterPointVO> getScatter(String warehouseCode, String factor) {
        return analyticsClient.getScatter(warehouseCode, factor);
    }

    private void enrichTierInfo(FactorRankVO f) {
        double absCorr = f.getCorrelation() != null ? Math.abs(f.getCorrelation().doubleValue()) : 0;
        if (absCorr >= 0.7) {
            f.setTier(1);
            f.setTierLabel("核心驱动");
        } else if (absCorr >= 0.4) {
            f.setTier(2);
            f.setTierLabel("重要影响");
        } else {
            f.setTier(3);
            f.setTierLabel("结构影响");
        }
        f.setApplicationTip(APPLICATION_TIPS.getOrDefault(f.getFactorName(), ""));
    }
}
