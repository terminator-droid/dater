package com.dudev.datingapp.swipe.repository;

import com.dudev.datingapp.swipe.entity.Swipe;
import com.dudev.datingapp.swipe.entity.SwipeDirection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, UUID> {

    boolean existsBySwiperIdAndSwipedIdAndDirectionAndDate(
            UUID swiperId, UUID swipedId, SwipeDirection direction, LocalDate date);

    void deleteAllBySwiperIdOrSwipedId(UUID swiperId, UUID swipedId);
}
