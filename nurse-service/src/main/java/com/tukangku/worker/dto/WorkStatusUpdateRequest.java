package com.tukangku.worker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WorkStatusUpdateRequest {

    @NotBlank
    @Pattern(regexp = "OPEN|CLOSED", message = "Status harus OPEN atau CLOSED")
    private String status;
}
