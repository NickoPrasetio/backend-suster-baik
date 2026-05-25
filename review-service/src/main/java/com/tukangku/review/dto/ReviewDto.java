package com.tukangku.review.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class ReviewDto {
    private String id;
    private String workerId;
    private String userId;
    private String userName;
    private Integer rating;
    private String comment;
    private String date;
    private String bookingId;
    private List<String> photoUrls;
}
