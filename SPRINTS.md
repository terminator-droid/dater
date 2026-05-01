# Sprint Plan — Dating App MVP

Статусы: `[ ]` не начато · `[~]` в процессе · `[x]` готово

---

## Sprint 1 — Фундамент + Аутентификация ✅

### Инфраструктура
- [x] `docker-compose.yml` (PostgreSQL, MongoDB, Redis, Kafka)
- [x] `application.properties` / `application-local.properties`
- [x] Spring-конфиги: `JpaConfig`, `RedisConfig`
- [x] `BaseEntity` (UUID id, created_at, updated_at)
- [x] Глобальный `@RestControllerAdvice` + стандартный `ApiResponse<T>`

### Пользователи и Auth
- [x] Первая Liquibase миграция: таблица `users`
- [x] `User` entity + `UserRepository`
- [x] `POST /api/v1/auth/register` + `POST /api/v1/auth/login` → JWT
- [x] JWT-фильтр, `UserDetailsService`, `SecurityConfig`
- [x] `POST /api/v1/auth/refresh`
- [x] `Photo` entity + миграция + `POST/DELETE /api/v1/users/me/photos`
- [x] `GET /api/v1/users/me`, `PUT /api/v1/users/me`

---

## Sprint 2 — Заведения и Планы ✅

- [x] Миграция таблицы `venues` + seed-данные (12 баров Москвы)
- [x] `Venue` entity, `VenueRepository`, `VenueService`
- [x] `GET /api/v1/venues`, `GET /api/v1/venues/{id}`
- [x] `ConversationTopic` document (MongoDB) + seed-данные (25 тем)
- [x] `GET /api/v1/topics?category=` — каталог тем для выбора
- [x] Миграции таблиц `evening_plans` + `evening_plan_topics`
- [x] `EveningPlan` + `EveningPlanTopic` entities, сервис
- [x] `POST /api/v1/plans` → сохранить в PG (с drink_tonight + topic_ids) + `GEOADD` в Redis с TTL
- [x] `GET /api/v1/plans?date=`, `DELETE /api/v1/plans/{id}`

---

## Sprint 3 — Дискавери и Свайпы ✅

- [x] `DiscoveryService`: `GEORADIUS` из Redis → фильтр уже-свайпнутых (Redis SET)
- [x] `GET /api/v1/discover?venue_id=&date=` → карточки: фото + topic tags (без имени/возраста)
- [x] Миграция таблицы `swipes` + уникальный индекс `(swiper_id, swiped_id, venue_id, date)`
- [x] `Swipe` entity, репозиторий, сервис
- [x] `POST /api/v1/swipes` → сохранить в PG + добавить в Redis SET + publish `SwipeEvent` в Kafka
- [x] Rate limiting свайпов через Redis (100 свайпов в день, сброс в полночь UTC)

---

## Sprint 4 — Матчинг ✅

- [x] Kafka consumer на топик `swipe-events`
- [x] Проверка обратного LIKE в PG → создать `Match` + publish `MatchEvent`
- [x] Миграция таблицы `matches`
- [x] `Match` entity, репозиторий, сервис
- [x] `GET /api/v1/matches`
- [x] `GET /api/v1/matches/{id}` → drink_tonight партнёра + его темы (из MongoDB) + venue
- [x] `PATCH /api/v1/matches/{id}/status` → отметить MET

---

## Sprint 5 — Уведомления, Статус и Финальный polish

- [ ] Kafka consumer на `match-events` → `NotificationService` (stub → лог, потом FCM)
- [ ] Онлайн-статус: `user:{id}:online` TTL в Redis через `HandlerInterceptor`
- [ ] `@Scheduled` job: перевод просроченных матчей в `EXPIRED`
- [ ] OAuth2 вход через Google (`POST /api/v1/auth/oauth2/google`)
- [ ] Integration-тесты с Testcontainers: регистрация, создание плана, свайп→матч
- [ ] `@WebMvcTest` для ключевых контроллеров
