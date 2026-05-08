package com.dudev.datingapp.swipe.entity;

import com.dudev.datingapp.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "swipes")
@Getter
@Setter
@NoArgsConstructor
public class Swipe extends BaseEntity {

    @Column(name = "swiper_id", nullable = false)
    private UUID swiperId;

    @Column(name = "swiped_id", nullable = false)
    private UUID swipedId;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SwipeDirection direction;

    @Column(nullable = false)
    private LocalDate date;
}
