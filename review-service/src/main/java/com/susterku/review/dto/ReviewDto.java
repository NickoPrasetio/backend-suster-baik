package com.susterku.review.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ReviewDto {
    private String id;
    private String nurseId;
    private String userId;
    private String userName;
    private Integer rating;
    private String comment;
    private String date;
}
