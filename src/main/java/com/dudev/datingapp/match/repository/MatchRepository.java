package com.dudev.datingapp.match.repository;

import com.dudev.datingapp.match.entity.Match;
import com.dudev.datingapp.match.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    @Query("SELECT m FROM Match m WHERE m.user1Id = :userId OR m.user2Id = :userId ORDER BY m.date DESC")
    List<Match> findAllForUser(@Param("userId") UUID userId);

    @Query("SELECT m FROM Match m WHERE m.id = :id AND (m.user1Id = :userId OR m.user2Id = :userId)")
    Optional<Match> findByIdForUser(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT COUNT(m) > 0 FROM Match m WHERE " +
           "((m.user1Id = :a AND m.user2Id = :b) OR (m.user1Id = :b AND m.user2Id = :a)) " +
           "AND m.date = :date")
    boolean existsByUsersAndDate(@Param("a") UUID a, @Param("b") UUID b, @Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE Match m SET m.status = :newStatus WHERE m.status = :oldStatus AND m.date < :cutoff")
    int updateStatus(@Param("cutoff") LocalDate cutoff,
                     @Param("oldStatus") MatchStatus oldStatus,
                     @Param("newStatus") MatchStatus newStatus);

    @Modifying
    @Query("DELETE FROM Match m WHERE m.user1Id = :userId OR m.user2Id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
