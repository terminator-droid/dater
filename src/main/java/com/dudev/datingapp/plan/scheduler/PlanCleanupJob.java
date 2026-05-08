package com.dudev.datingapp.plan.scheduler;

import com.dudev.datingapp.plan.entity.EveningPlan;
import com.dudev.datingapp.plan.repository.EveningPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/// Cleanup of past plans that nobody is referencing anymore. Runs at 06:00 —
/// long after the evening is actually over (people meet through the night
/// into the next morning), but before normal daytime traffic begins.
///
/// Plans with at least one match attached are intentionally left in place:
/// the match references plan1Id/plan2Id as a NOT NULL FK, and we want
/// match history (and the partner-venue lookup) to keep working.
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanCleanupJob {

    private final EveningPlanRepository planRepository;

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void deleteOrphanPastPlans() {
        LocalDate cutoff = LocalDate.now();
        List<EveningPlan> orphans = planRepository.findOrphanPastPlans(cutoff);
        if (orphans.isEmpty()) {
            return;
        }
        planRepository.deleteAll(orphans);
        log.info("Deleted {} orphan past plans (date < {})", orphans.size(), cutoff);
    }
}
