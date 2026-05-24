package com.tukangku.booking.client;

import lombok.Data;

/**
 * Subset data WorkerEntity yang dibutuhkan booking-service.
 * Diisi dari respons nurse-service /internal/workers/{id}/book
 */
@Data
public class WorkerInfoDto {
    private String id;
    private String name;
    private String avatar;
    private String workStatus;
}
