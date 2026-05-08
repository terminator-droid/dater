package com.dudev.datingapp.match.entity;

import com.dudev.datingapp.common.BaseEntity;
import com.dudev.datingapp.plan.entity.EveningPlan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class Match extends BaseEntity {

    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(name = "plan1_id", nullable = false)
    private UUID plan1Id;

    @Column(name = "plan2_id", nullable = false)
    private UUID plan2Id;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MatchStatus status = MatchStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan1_id", insertable = false, updatable = false)
    private EveningPlan plan1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan2_id", insertable = false, updatable = false)
    private EveningPlan plan2;
}
