package com.susterku.nurse.dto;

import lombok.Data;

@Data
public class RatingUpdateRequest {
    private Double newRating;
    private Integer totalReviews;
}
