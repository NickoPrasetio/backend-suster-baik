package com.tukangku.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class BookingDto {
    private String id;
    private String workerId;
    private String workerName;
    private String workerAvatar;
    private String customerId;
    private String customerName;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private LocalDate bookingDate;
    private String startTime;
    private Integer durationDays;
    private String paymentMethod;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
}
