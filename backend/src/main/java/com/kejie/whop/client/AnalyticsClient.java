package com.kejie.whop.client;

import com.kejie.whop.model.vo.CorrelationMatrixVO;
import com.kejie.whop.model.vo.FactorRankVO;
import com.kejie.whop.model.vo.ScatterPointVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AnalyticsClient {

    private final RestClient restClient;

    public AnalyticsClient(@Value("${analytics.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<FactorRankVO> getFactors(String warehouseCode) {
        try {
            return restClient.get()
                    .uri("/api/impact/factors?warehouseCode={code}", warehouseCode)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to fetch factors from analytics service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public CorrelationMatrixVO getCorrelation(String warehouseCode) {
        try {
            return restClient.get()
                    .uri("/api/impact/correlation?warehouseCode={code}", warehouseCode)
                    .retrieve()
                    .body(CorrelationMatrixVO.class);
        } catch (Exception e) {
            log.warn("Failed to fetch correlation from analytics service: {}", e.getMessage());
            return new CorrelationMatrixVO();
        }
    }

    public List<ScatterPointVO> getScatter(String warehouseCode, String factor) {
        try {
            return restClient.get()
                    .uri("/api/impact/scatter?warehouseCode={code}&factor={factor}", warehouseCode, factor)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to fetch scatter from analytics service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
