package com.susterku.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotBlank
    private String nurseId;

    @NotBlank
    private String userName;

    @NotNull @Min(1) @Max(5)
    private Integer rating;

    @NotBlank @Size(min = 5, max = 500)
    private String comment;
}
