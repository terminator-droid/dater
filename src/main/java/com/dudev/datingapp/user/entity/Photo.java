package com.dudev.datingapp.user.entity;

import com.dudev.datingapp.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "photos")
@Getter
@Setter
@NoArgsConstructor
public class Photo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private int position;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}
