package com.dudev.datingapp.plan.entity;

import com.dudev.datingapp.common.BaseEntity;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.venue.entity.Venue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evening_plans")
@Getter
@Setter
@NoArgsConstructor
public class EveningPlan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "drink_tonight")
    private String drinkTonight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.PLANNED;

    @Column(name = "appearance_hint", length = 200)
    private String appearanceHint;

    @ElementCollection
    @CollectionTable(
            name = "evening_plan_topics",
            joinColumns = @JoinColumn(name = "plan_id")
    )
    @Column(name = "topic_id")
    private List<String> topicIds = new ArrayList<>();
}
