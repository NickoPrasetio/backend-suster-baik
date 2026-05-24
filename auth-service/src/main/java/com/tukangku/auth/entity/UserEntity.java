package com.tukangku.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private String role = "ROLE_USER";

    private String avatar;

    @Column(nullable = false)
    @Builder.Default
    private String userType = "CUSTOMER";

    /** LOCAL | GOOGLE */
    @Column(nullable = false)
    @Builder.Default
    private String provider = "LOCAL";

    /** Google subject ID — null untuk user yang daftar manual */
    @Column(name = "google_id", unique = true)
    private String googleId;

    private Double latitude;
    private Double longitude;
}
