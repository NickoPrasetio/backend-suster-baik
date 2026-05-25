package com.tukangku.review.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDate date;

    /** ID booking yang terkait — opsional untuk review dengan foto */
    @Column(name = "booking_id")
    private String bookingId;

    /** URL foto hasil kerja yang diupload customer ke MinIO */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_photos", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "photo_url", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();
}
