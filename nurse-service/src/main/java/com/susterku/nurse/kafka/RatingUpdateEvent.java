package com.susterku.nurse.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingUpdateEvent {
    private String nurseId;
    private double newRating;
    private int totalReviews;
}
