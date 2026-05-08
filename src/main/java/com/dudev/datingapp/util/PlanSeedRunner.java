package com.dudev.datingapp.util;

import com.dudev.datingapp.plan.dto.CreatePlanDto;
import com.dudev.datingapp.plan.service.PlanService;
import com.dudev.datingapp.user.entity.User;
import com.dudev.datingapp.user.repository.UserRepository;
import com.dudev.datingapp.venue.entity.Venue;
import com.dudev.datingapp.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class PlanSeedRunner implements CommandLineRunner {

    private final PlanService planService;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        try {
            seedPlans();
        } catch (Exception e) {
            log.error("Seed failed", e);
        }
    }

    private void seedPlans() {
        LocalDate date = LocalDate.now();
        List<User> users = userRepository.findAll();
        List<Venue> venues = venueRepository.findAll();

        log.info("Seed started: users={}, venues={}", users.size(), venues.size());

        if (users.isEmpty() || venues.isEmpty()) {
            log.warn("Seed skipped because users or venues are empty");
            return;
        }

        String[] drinks = {"негрони", "вино", "IPA", "апероль", "джин-тоник", "мартини", "спритц"};
        String[] hints = {
                "синяя куртка у барной стойки",
                "в черном пиджаке",
                "у окна",
                "за столиком у входа",
                "смотрю в телефон",
                "у бара слева"
        };

        for (User user : users) {
            Venue venue = venues.get(random.nextInt(venues.size()));
            CreatePlanDto dto = new CreatePlanDto(
                    venue.getId(),
                    date,
                    drinks[random.nextInt(drinks.length)],
                    List.of("69f4e2dd3f6d9f07dde186c9", "69f4e2dd3f6d9f07dde186d0"),
                    hints[random.nextInt(hints.length)]
            );

            try {
                log.info("Creating plan for user={} venue={}", user.getId(), venue.getId());
                planService.createPlan(user.getId(), dto);
            } catch (Exception e) {
                log.warn("Skip user={} venue={} because: {}", user.getId(), venue.getId(), e.getMessage());
            }
        }

        log.info("Seed completed");
    }
}