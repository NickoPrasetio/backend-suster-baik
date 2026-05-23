package com.tukangku.worker.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class WorkerDto {
    private String id;
    private String name;
    private String avatar;
    private Integer age;
    private Integer experience;
    private Double rating;
    private Integer totalReviews;
    private List<String> specializations;
    private String location;
    private BigDecimal pricePerDay;
    private Boolean isAvailable;
    private String bio;
}
