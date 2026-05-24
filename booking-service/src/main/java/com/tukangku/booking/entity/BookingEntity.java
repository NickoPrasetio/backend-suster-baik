package com.tukangku.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** ID worker dari worker-service (reference, bukan FK lintas DB) */
    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private String startTime;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "payment_method", nullable = false)
    @Builder.Default
    private String paymentMethod = "CASH";

    /** PENDING | CONFIRMED | CANCELLED | COMPLETED */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Cache nama & avatar tukang agar tidak perlu join lintas service setiap query */
    @Column(name = "worker_name")
    private String workerName;

    @Column(name = "worker_avatar", columnDefinition = "TEXT")
    private String workerAvatar;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
