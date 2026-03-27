package com.kejie.whop.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportGenerateRequest {

    @NotBlank
    private String warehouseCode;

    @NotBlank
    private String startMonth;

    @NotBlank
    private String endMonth;
}
