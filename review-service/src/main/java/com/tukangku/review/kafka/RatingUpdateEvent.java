package com.tukangku.review.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingUpdateEvent {
    private String workerId;
    private double newRating;
    private int totalReviews;
}
