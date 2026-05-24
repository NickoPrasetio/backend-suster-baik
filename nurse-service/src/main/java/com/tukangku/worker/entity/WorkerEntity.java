package com.tukangku.worker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String avatar;

    private Integer age;

    private Integer experience;

    @Column(nullable = false)
    private Double rating;

    @Column(name = "total_reviews")
    private Integer totalReviews;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "worker_specializations", joinColumns = @JoinColumn(name = "worker_id"))
    @Column(name = "specialization")
    @Builder.Default
    private List<String> specializations = new ArrayList<>();

    private String location;

    @Column(name = "price_per_day")
    private BigDecimal pricePerDay;

    @Column(name = "is_available")
    private Boolean isAvailable;

    /** OPEN | CLOSED — status yang dikendalikan tukang sendiri */
    @Column(name = "work_status", nullable = false)
    @Builder.Default
    private String workStatus = "OPEN";

    /** ID user di auth-service, digunakan untuk lookup saat tukang update datanya sendiri */
    @Column(name = "auth_user_id", unique = true)
    private String authUserId;

    private Double latitude;

    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String bio;
}
