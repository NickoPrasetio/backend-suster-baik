package com.tukangku.worker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TukangLocationUpdateRequest {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
