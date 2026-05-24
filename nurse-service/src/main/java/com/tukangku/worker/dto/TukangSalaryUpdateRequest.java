package com.tukangku.worker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TukangSalaryUpdateRequest {

    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "Gaji harus lebih dari 0")
    private BigDecimal pricePerDay;
}
