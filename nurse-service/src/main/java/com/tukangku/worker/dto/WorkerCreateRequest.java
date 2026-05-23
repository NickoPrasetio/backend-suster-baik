package com.tukangku.worker.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WorkerCreateRequest {
    private String name;
    private String avatar;
    private Integer age;
    private Integer experience;
    private List<String> specializations;
    private String location;
    private BigDecimal pricePerDay;
    private Boolean isAvailable;
    private String bio;
}
