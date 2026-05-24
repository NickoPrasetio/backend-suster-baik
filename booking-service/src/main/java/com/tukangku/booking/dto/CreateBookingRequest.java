package com.tukangku.booking.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateBookingRequest {

    @NotBlank
    private String workerId;

    @NotBlank
    private String customerName;

    @NotBlank
    private String address;

    @NotBlank
    private String city;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @NotNull
    @FutureOrPresent(message = "Tanggal booking tidak boleh di masa lalu")
    private LocalDate bookingDate;

    @NotBlank
    private String startTime;

    @NotNull
    @Min(value = 1,  message = "Durasi minimal 1 hari")
    @Max(value = 30, message = "Durasi maksimal 30 hari")
    private Integer durationDays;

    @NotBlank
    private String paymentMethod;

    private String notes;
}
