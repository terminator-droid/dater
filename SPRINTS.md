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

## Sprint 5 — Уведомления, Статус и Финальный polish ✅

- [x] Kafka consumer на `match-events` → `NotificationService` (stub → лог, потом FCM)
- [x] Онлайн-статус: `user:{id}:online` TTL в Redis через `HandlerInterceptor`
- [x] `@Scheduled` job: перевод просроченных матчей в `EXPIRED`
- [x] OAuth2 вход через Google (`POST /api/v1/auth/oauth2/google`)
- [x] Integration-тесты с Testcontainers: регистрация, создание плана, свайп→матч
- [~] `@WebMvcTest` для ключевых контроллеров (отложено)

---

## Sprint 6 — OpenAPI-документация и Профиль пользователя

- [x] `@Operation` / `@Tag` аннотации на всех контроллерах (Swagger UI)
- [x] `GET /api/v1/users/{id}` — публичный профиль (фото, без имени/возраста)
- [x] `GET /api/v1/users/me/online` — проверить свой онлайн-статус
- [x] Валидация `RegisterRequest`: формат телефона (`@Pattern`), возраст ≥ 18 лет, `@Past` на birthDate
- [x] `DELETE /api/v1/users/me` — удаление аккаунта (каскад: планы, свайпы, матчи, фото, Redis)
- [x] `PATCH /api/v1/users/me/location` — обновить гео-позицию вручную (для тестирования дискавери)
- [x] `GET /api/v1/venues/nearby?lat=&lon=&radius=` — заведения поблизости (Haversine)
- [x] S3-хранилище фото: MinIO в docker-compose, AWS SDK v2, `S3Config`, `S3BucketInitializer`

---

## Sprint 7 — Bugfix: критические ошибки и недостающий функционал

### Критические баги

- [x] **`AdminInitializer`** — исправить swapped fields: `setPassword` кодирует `adminUsername` вместо `adminPassword`, `setName` ставит `adminPassword` вместо имени; phone `"12"` не проходит `@Pattern` → задать корректные значения из `@Value`
- [x] **Google OAuth — переполнение VARCHAR(20)** — `"google:" + sub` (~28 символов) не влезает в `users.phone VARCHAR(20)`; расширить колонку до `VARCHAR(64)` через новую Liquibase-миграцию
- [x] **Kafka poison pill в `MatchService.createMatchIfAbsent`** — если план не найден, бросается `ResourceNotFoundException` → consumer застревает на одном offset; обернуть в `try/catch`, логировать и пропускать сообщение
- [x] **Отсутствует `ON DELETE CASCADE` на FK в `swipes` и `matches`** — добавить каскадное удаление через новую Liquibase-миграцию (`ALTER TABLE swipes ADD CONSTRAINT ... ON DELETE CASCADE`, аналогично для `matches`)

### Отсутствующий функционал MVP

- [x] **`POST /api/v1/auth/logout`** — удалять refresh-токен из Redis; без этого скомпрометированный токен нельзя инвалидировать
- [x] **Валидация типа файла при загрузке фото** — в `PhotoController` / `PhotoStorageService` проверять MIME-тип (`image/jpeg`, `image/png`, `image/webp`, `image/gif`); отклонять всё остальное с 400
- [x] **`UpdateProfileDto` — валидация birthDate** — добавить `@Past` и проверку возраста ≥ 18 лет в `updateProfile` (аналогично `RegisterRequest` + `AuthService.register`)

### Мелкие проблемы

- [x] **`SecurityConfig` — дублирующийся `requestMatchers` для Swagger** — убрать дублирующийся блок `permitAll` для `/swagger-ui/**` и `/v3/api-docs/**`
- [x] **Kafka consumers — отсутствует `try/catch`** — `SwipeEventConsumer` и `NotificationConsumer` не оборачивают тело в `try/catch`; любое runtime-исключение = бесконечный retry; добавить `try/catch (Exception e)` с `log.error`

---

## Sprint 8 — Appearance hint и полный адрес заведения в матче

- [x] **`appearance_hint` в плане** — добавить поле `VARCHAR(200)` в `evening_plans` (миграция `010`); заполняется при создании плана; необязательное
- [x] **`CreatePlanDto.appearanceHint`** — опциональное поле `@Size(max = 200)`, отдаётся в `PlanDto`
- [x] **`MatchDetailDto.venueAddress`** — добавить адрес заведения в детали матча, чтобы пользователь знал, куда идти (раньше был только `venueName`)
- [x] **`MatchDetailDto.partnerAppearanceHint`** — добавить подсказку для узнавания партнёра из его плана

---

## Sprint 9 — Множественные планы и активный план ✅

Пользователь может составить несколько планов (разные заведения / даты), но **только один может быть ACTIVE на конкретную дату**. Активный план — единственный, который участвует в дискавери (геопозиция в Redis). Также нужна возможность просматривать все планы и корректно удалять план вместе с зависимыми матчами.

### Модель данных и миграции

- [ ] **Миграция `011_plan_active_uniqueness_and_match_cascade.sql`**
  - Partial unique index: `CREATE UNIQUE INDEX uq_active_plan_per_user_date ON evening_plans (user_id, date) WHERE status = 'ACTIVE';` — БД-гарантия одного активного плана в день.
  - `ALTER TABLE matches DROP CONSTRAINT matches_plan1_id_fkey, ADD CONSTRAINT matches_plan1_id_fkey FOREIGN KEY (plan1_id) REFERENCES evening_plans (id) ON DELETE CASCADE;` (то же для `plan2_id`) — чтобы удаление плана каскадно удаляло зависимые матчи.

### Сервис и репозиторий

- [ ] **`EveningPlanRepository`**
  - `findAllByUserId(UUID)` — все планы пользователя (с `JOIN FETCH venue, topicIds`).
  - `@Modifying @Query` для массового перевода активных планов пользователя на дате в `PLANNED` (используется при активации другого плана).
- [ ] **`PlanService.createPlan`**
  - Если у пользователя на эту дату ещё нет ACTIVE-плана — новый план создаётся со статусом `ACTIVE` и регистрируется в Redis GEO (текущее поведение).
  - Если ACTIVE-план уже есть — новый план создаётся со статусом `PLANNED`, в GEO не добавляется.
- [ ] **`PlanService.activatePlan(userId, planId)`**
  - Загрузить план, проверить владение.
  - Перевести все остальные ACTIVE-планы пользователя на эту дату в `PLANNED` и удалить старого юзера из `geo:users:{date}`.
  - Перевести этот план в `ACTIVE`, добавить пользователя в `geo:users:{date}` с координатами venue этого плана.
- [ ] **`PlanService.getAllPlans(userId)`** — вернуть все планы пользователя (отсортировано по дате DESC).
- [ ] **`PlanService.deletePlan`** — текущая JPA-каскадная логика остаётся; если удаляется ACTIVE-план — также убрать пользователя из `geo:users:{date}`. Каскад на матчи теперь дублируется на уровне БД (миграция 011).

### Контроллер и DTO

- [ ] **`POST /api/v1/plans/{planId}/activate`** — активировать конкретный план; возвращает обновлённый `PlanDto`.
- [ ] **`GET /api/v1/plans`** (без параметров) — все планы пользователя.
- [ ] **`GET /api/v1/plans?date=`** — планы на конкретную дату (без изменений).
- [ ] **`PlanDto`** — поле `status` уже отдаётся; убедиться, что фронт может его читать.

### Проверки

- [ ] Удаление плана действительно удаляет связанные `matches` (JPA cascade + DB ON DELETE CASCADE).
- [ ] Нельзя одновременно иметь два ACTIVE-плана на одну дату (ловится partial unique index).
- [ ] После активации другого плана дискавери начинает работать с новой venue.
