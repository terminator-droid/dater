package com.dudev.datingapp.match.scheduler;

import com.dudev.datingapp.match.entity.MatchStatus;
import com.dudev.datingapp.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchExpiryJob {

    private final MatchRepository matchRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireOldMatches() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        int count = matchRepository.updateStatus(yesterday, MatchStatus.PENDING, MatchStatus.EXPIRED);
        log.info("Expired {} old matches (date < {})", count, yesterday);
    }
}
