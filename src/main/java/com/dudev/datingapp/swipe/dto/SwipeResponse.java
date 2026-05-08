package com.dudev.datingapp.swipe.dto;

import java.util.UUID;

/// `matchId` is non-null only when this swipe immediately produced a mutual
/// match (the other user had already liked the swiper). The client uses it to
/// trigger the match animation without waiting for the next /matches poll.
public record SwipeResponse(UUID swipeId, UUID matchId) {
}
