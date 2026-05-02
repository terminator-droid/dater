# Dating App — Backend MVP

Spring Boot 4 / Java 17 монолит для приложения знакомств на основе вечерних планов. Пользователи создают план "иду сегодня в бар X", свайпают друг друга анонимно по темам интересов и при взаимном лайке получают матч с деталями встречи. Имя и возраст партнёра скрыты до физической встречи.

---

## Стек технологий

| Слой | Технология |
|---|---|
| Framework | Spring Boot 4.0 / Java 17 |
| Основная БД | PostgreSQL 17 (миграции Liquibase) |
| Документы | MongoDB 7 (темы для разговора) |
| Кеш / Гео | Redis 7 (GEORADIUS, rate limiting, онлайн-статус) |
| Очереди | Kafka (KRaft mode, 3 топика) |
| Хранилище фото | MinIO (S3-совместимый) |
| Безопасность | JWT + Spring Security + Google OAuth2 |
| Документация | Springdoc OpenAPI (Swagger UI) |
| Тесты | JUnit 5, Testcontainers, MockMvc |

---

## Быстрый старт

### Запуск в Docker (рекомендуется)

```bash
docker compose up --build
```

Поднимает все зависимости (PostgreSQL, MongoDB, Redis, Kafka, MinIO) и само приложение. Миграции Liquibase применяются автоматически при старте.

Приложение доступно на `http://localhost:8081`.
Swagger UI: `http://localhost:8081/swagger-ui/index.html`
MinIO Console: `http://localhost:9001` (логин: `minio` / `minio123`)

### Локальная разработка

```bash
# Поднять только инфраструктуру
docker compose up postgres mongodb redis kafka minio -d

# Запустить приложение
./mvnw spring-boot:run
```

### Сборка и тесты

```bash
# Сборка
./mvnw clean package

# Все тесты (поднимает Testcontainers — требует Docker)
./mvnw test

# Один тест-класс
./mvnw test -Dtest=AuthFlowIntegrationTest

# Один метод
./mvnw test -Dtest=AuthFlowIntegrationTest#register_login_refresh_allSucceed
```

---

## Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/datingapp` | URL PostgreSQL |
| `DB_USERNAME` | `datingapp` | Пользователь БД |
| `DB_PASSWORD` | `datingapp` | Пароль БД |
| `MONGO_URI` | `mongodb://localhost:27017/datingapp` | URI MongoDB |
| `REDIS_HOST` | `localhost` | Хост Redis |
| `KAFKA_SERVERS` | `localhost:9092` | Bootstrap-серверы Kafka |
| `S3_ENDPOINT` | `http://localhost:9000` | Endpoint MinIO/S3 |
| `S3_ACCESS_KEY` | `minio` | Access key |
| `S3_SECRET_KEY` | `minio123` | Secret key |
| `S3_BUCKET` | `dating-app-photos` | Имя бакета |
| `JWT_SECRET` | встроенный | HMAC-секрет для JWT (замените в prod!) |

---

## Архитектура модулей

Каждый доменный модуль следует схеме `entity → repository → service → dto → controller`. MongoDB-документы живут в `document/` вместо `entity/`. Kafka-продюсеры в `event/`, консьюмеры в `consumer/`.

```
com.dudev.datingapp
├── auth          — регистрация, вход, токены, Google OAuth2
├── user          — профили, фотографии (S3), гео-позиция, удаление аккаунта
├── venue         — справочник заведений, поиск поблизости (Haversine)
├── plan          — вечерние планы (PG + Redis GEOADD)
├── discovery     — поиск людей (Redis GEORADIUS → фильтр свайпов → MongoDB темы)
├── swipe         — свайпы (PG + Redis rate limit + Kafka)
├── match         — матчинг (Kafka consumer + @Scheduled expiration)
├── topic         — темы для разговора (MongoDB)
├── notification  — уведомления (Kafka consumer, stub → FCM)
├── security      — JWT-фильтр, онлайн-статус (Redis TTL)
└── config        — S3, WebMvc, Kafka конфигурации
```

---

## Описание модулей

### auth — Аутентификация

Пользователь регистрируется с телефоном и паролем → получает **JWT access token** (15 минут) и **refresh token** (30 дней, хранится в Redis). При каждом запросе `JwtAuthenticationFilter` проверяет заголовок `Authorization: Bearer <token>` и устанавливает Principal в SecurityContext.

Google OAuth: клиент получает `id_token` от Google → отправляет на `/api/v1/auth/oauth2/google` → сервер верифицирует через Google tokeninfo API → ищет или создаёт пользователя.

**Endpoints:** `POST /register` · `POST /login` · `POST /refresh` · `POST /logout` · `POST /oauth2/google`

---

### user — Пользователи и профили

Хранит профили в PostgreSQL. Фотографии загружаются в **MinIO**: в БД хранится только `s3_key`, `PhotoStorageService` строит публичный URL. Поддерживаемые форматы: JPEG, PNG, WebP, GIF. При удалении аккаунта каскадно чистятся планы, свайпы, матчи, S3-объекты и Redis-ключи.

`PATCH /users/me/location` обновляет гео-позицию в Redis без создания плана — удобно для тестирования дискавери.

**Endpoints:** `GET /me` · `PUT /me` · `DELETE /me` · `POST /me/photos` · `DELETE /me/photos/{id}` · `GET /{id}` · `GET /me/online` · `PATCH /me/location`

---

### venue — Заведения

Статический справочник из 12 баров Москвы (Патриаршие, Красный Октябрь, Китай-город и др.), засеянных через Liquibase-миграцию. `GET /venues/nearby?lat=&lon=&radius=` считает расстояние по формуле Haversine прямо в сервисе.

**Endpoints:** `GET /venues` · `GET /venues/{id}` · `GET /venues/nearby`

---

### plan — Вечерние планы

Центральный объект MVP. Пользователь говорит: "Сегодня иду в бар X, буду пить Y, хочу говорить о темах T1, T2". При создании плана происходит **двойная запись**:

1. `EveningPlan` → PostgreSQL (все поля, topic_ids как JSON)
2. `GEOADD geo:users:{date} lon lat userId` → Redis с TTL до полуночи

Redis используется только для быстрого геопоиска. TTL автоматически чистит устаревшие данные.

Поле **`appearanceHint`** (до 200 символов, необязательное) — короткое описание, как тебя найти в баре: "синяя куртка, у барной стойки". Отображается партнёру в деталях матча, чтобы можно было подойти и познакомиться.

**Endpoints:** `POST /plans` · `GET /plans?date=` · `DELETE /plans/{id}`

---

### discovery — Дискавери

Показывает анонимные карточки людей поблизости. Три шага:

**1. Redis GEORADIUS** — находит UUID пользователей в радиусе 2 км с активным планом на дату:
```
GEORADIUS geo:users:2026-05-02 37.5931 55.7644 2 km
```

**2. Фильтр** — из Redis SET `swiped:{userId}` убираются уже-свайпнутые.

**3. Сборка карточек** — для оставшихся загружается план из PG, `topic_ids` резолвятся в полный текст из MongoDB. Возвращаются фото и теги интересов. **Имя и возраст скрыты** — анонимность до встречи.

**Endpoint:** `GET /discover?venue_id=&date=`

---

### swipe — Свайпы

`POST /swipes` с `{ "swipedId": "...", "direction": "LIKE|PASS" }`:

1. Сохраняет свайп в PostgreSQL (уникальный индекс на `swiper+swiped+venue+date`)
2. Добавляет `swipedId` в Redis SET (для фильтрации в дискавери)
3. **Rate limiting**: Redis счётчик `swipe:count:{userId}:{date}` — максимум 100 свайпов в день, сброс в полночь UTC
4. Публикует `SwipeEvent` в Kafka топик `swipe-events`

**Endpoint:** `POST /swipes`

---

### match — Матчинг

**Kafka consumer** слушает `swipe-events`. При получении LIKE:
- Проверяет в PostgreSQL: есть ли обратный LIKE?
- Если да — создаёт `Match` в PG → публикует `MatchEvent` в `match-events`
- Если план не найден — логирует и пропускает (защита от poison pill)

`GET /matches/{id}` объединяет данные из двух источников: **PG** (match + план партнёра) + **MongoDB** (полный текст тем по `topic_ids`). Возвращает:
- **`venueAddress`** — полный адрес заведения, куда нужно подойти
- **`partnerDrinkTonight`** — что будет пить партнёр
- **`partnerTopicTags`** — его темы интересов
- **`partnerAppearanceHint`** — как его узнать ("красная шапка у входа")

Имя и возраст партнёра по-прежнему скрыты.

`@Scheduled` джоб раз в час переводит матчи старше суток в статус `EXPIRED`.

**Endpoints:** `GET /matches` · `GET /matches/{id}` · `PATCH /matches/{id}/status`

---

### topic — Темы для разговора

25 тем в MongoDB. Структура документа гибкая (теги, категории), без реляционных связей — поэтому MongoDB, а не PostgreSQL. Пользователь выбирает темы при создании плана.

**Endpoint:** `GET /topics?category=`

---

### notification — Уведомления

Kafka consumer слушает `match-events` и вызывает `NotificationService`. Сейчас это заглушка — логирует событие. Архитектурно место для интеграции FCM/APNs. Consumer обёрнут в `try/catch` — падение не блокирует Kafka offset.

---

### security — Сквозная безопасность

`OnlineStatusInterceptor` перехватывает каждый запрос к `/api/**` и обновляет `user:{id}:online` в Redis с TTL 5 минут. Онлайн-статус обновляется автоматически без лишних вызовов.

---

## Ключевые потоки

### Свайп → Матч

```
POST /swipes
    │
    ├─ PG: INSERT swipes
    ├─ Redis: SADD swiped:{userId}
    └─ Kafka: publish SwipeEvent
                    │
            SwipeEventConsumer
                    │
            PG: обратный LIKE?
                    │
                    └─ да → INSERT matches + publish MatchEvent
                                              │
                                     MatchEventConsumer
                                              │
                                     NotificationService.log()
```

### Создание плана → Дискавери

```
POST /plans
    │
    ├─ PG: INSERT evening_plans
    └─ Redis: GEOADD geo:users:{date}
                    │
GET /discover ──────┘
    │
    ├─ Redis: GEORADIUS (кто рядом)
    ├─ Redis: SMEMBERS swiped:{userId} (исключить свайпнутых)
    ├─ PG: загрузить планы + фото
    └─ MongoDB: резолвить topic_ids → теги
```

---

## API

Полная документация доступна в Swagger UI после запуска:

```
http://localhost:8081/swagger-ui/index.html
```

### Примеры запросов

```bash
# Регистрация
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"phone":"+79001234567","password":"password123","name":"Alice","birthDate":"1995-06-15","gender":"FEMALE"}'

# Создать план
curl -X POST http://localhost:8081/api/v1/plans \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"venueId":"<uuid>","date":"2026-05-02","drinkTonight":"негрони","topicIds":["<id1>","<id2>"]}'

# Дискавери
curl http://localhost:8081/api/v1/discover?venue_id=<uuid>&date=2026-05-02 \
  -H "Authorization: Bearer <token>"

# Свайп
curl -X POST http://localhost:8081/api/v1/swipes \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"swipedId":"<uuid>","venueId":"<uuid>","direction":"LIKE"}'
```

---

## Тестирование

Используется три уровня тестов:

| Тип | Инструмент | Примеры |
|---|---|---|
| Unit | JUnit 5 + Mockito | `PlanServiceTest`, `SwipeServiceTest` |
| MVC | `@WebMvcTest` + MockMvc | `AuthControllerWebMvcTest`, `MatchControllerWebMvcTest` |
| Integration | `@SpringBootTest` + Testcontainers | `AuthFlowIntegrationTest`, `MatchCreationIntegrationTest` |

Интеграционные тесты поднимают реальные контейнеры (PostgreSQL, MongoDB, Kafka, Redis) через Testcontainers — никакой ручной настройки инфраструктуры не нужно.

---

## Структура БД

```
users ──< photos
  │
  ├──< evening_plans ──< evening_plan_topics
  │
  ├──< swipes (swiper_id + swiped_id + venue_id + date, ON DELETE CASCADE)
  │
  └──< matches (user1_id + user2_id + plan1_id + plan2_id, ON DELETE CASCADE)

venues (статика, seed в миграции 004)
```

MongoDB: коллекция `conversation_topics` (25 документов, seed при старте).
