package com.back.atlas.entity;

import com.back.atlas.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = true)
    private String password;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = true,name = "phone_number")
    private String phoneNumber;
    @Column(nullable = true)
    private String bio;
    @Column(nullable = true,name = "facebook_link")
    private String facebookLink;
    @Column(nullable = true,name = "instagram_link")
    private String instagramLink;
    @Column(nullable = true,name = "x_link")
    private String xLink;
    @Column(nullable = true,name = "linkedin_link")
    private String linkedinLink;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String providerId;

    @Column(nullable = true)
    private String avatar;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
