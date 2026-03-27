package com.kejie.whop.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CorrelationMatrixVO {

    private List<String> factors = new ArrayList<>();
    private List<List<Double>> matrix = new ArrayList<>();
}
