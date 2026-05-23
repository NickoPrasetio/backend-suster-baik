package com.susterku.nurse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nurses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NurseEntity {

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
    @CollectionTable(name = "nurse_specializations", joinColumns = @JoinColumn(name = "nurse_id"))
    @Column(name = "specialization")
    @Builder.Default
    private List<String> specializations = new ArrayList<>();

    private String location;

    @Column(name = "price_per_day")
    private BigDecimal pricePerDay;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @Column(columnDefinition = "TEXT")
    private String bio;
}
