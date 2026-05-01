# Архитектура — Offline Dating App

## 1. Общий подход

**Monolith-first.** На старте — единый Spring Boot сервис. Kafka и разделение хранилищ уже заложены, чтобы при необходимости вынести модули в отдельные сервисы без рефакторинга данных.

**Стек:** Spring Boot 4.0.6, Java 17, PostgreSQL, MongoDB, Redis, Kafka.

**Ключевая идея:** минимум цифрового взаимодействия. Приложение не заменяет живое знакомство — оно убирает тревогу перед ним. Никакого чата до встречи. Пользователям не показываются имя и возраст другого человека — только фото и несколько подсказок для начала разговора. Оба знают, что нравятся друг другу — этого достаточно, чтобы подойти.

---

## 2. Роли технологий

### PostgreSQL (основная БД)
Хранит всё, что имеет строгую схему и требует транзакций: пользователи, профили, бары, свайпы, мэтчи, планы на вечер. Миграции через Liquibase.

### MongoDB
Хранит каталог тем для разговоров (`ConversationTopic`) — гибкая структура с тегами, категориями и локализацией. Пользователь при создании плана выбирает темы из этого каталога.

### Redis
- **Geo-индекс:** `GEOADD` / `GEORADIUS` для поиска людей в радиусе от бара
- **Онлайн-статус:** TTL-ключи `user:{id}:online`
- **Кэш:** профили, списки баров по району, результаты дискавери
- **Rate limiting:** ограничение свайпов (скользящее окно)
- **Уже-свайпнутые:** Bloom filter или SET для быстрой проверки без запроса в PG

### Kafka
Три топика:
- `swipe-events` — каждый свайп публикуется сюда; consumer проверяет взаимность
- `match-events` — при мэтче генерируется событие; потребители: push-уведомления, аналитика
- `notification-events` — push/email уведомления (отложенная доставка)

---

## 3. Модель данных

### User (PostgreSQL)
| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK |
| phone | varchar | Уникальный, для входа |
| name | varchar | Имя — хранится, но **не показывается** в карточках |
| birth_date | date | Дата рождения — хранится, но **не показывается** в карточках |
| gender | enum | MALE / FEMALE / OTHER |
| created_at | timestamp | |
| updated_at | timestamp | |

### Photo (PostgreSQL)
| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK |
| user_id | UUID | FK → User |
| s3_key | varchar | Путь в S3 |
| position | int | Порядок в профиле |
| is_primary | boolean | Главное фото для карточки свайпа |

### Venue — бар/клуб (PostgreSQL)
| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK |
| name | varchar | Название |
| address | varchar | Адрес |
| latitude | double | Координаты |
| longitude | double | Координаты |
| area | varchar | Район (Патриаршие, Китай-город и т.д.) |
| category | enum | BAR / CLUB / LOUNGE |

### EveningPlan (PostgreSQL)
Пользователь заполняет перед выходом: куда идёт, что пьёт, о чём хочет поговорить.

| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK |
| user_id | UUID | FK → User |
| venue_id | UUID | FK → Venue |
| date | date | На какой вечер |
| drink_tonight | varchar | Что пью сегодня (свободный текст: «негрони», «IPA», «вино») |
| status | enum | PLANNED / ACTIVE / COMPLETED |
| created_at | timestamp | |

### EveningPlanTopic (PostgreSQL) — выбранные темы плана
| Поле | Тип | Описание |
|------|-----|----------|
| plan_id | UUID | FK → EveningPlan |
| topic_id | varchar | ID темы из MongoDB каталога |

### Swipe (PostgreSQL)
| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK |
| swiper_id | UUID | FK → User (кто свайпнул) |
| swiped_id | UUID | FK → User (кого свайпнули) |
| direction | enum | LIKE / PASS |
| venue_id | UUID | FK → Venue (в контексте какого бара) |
| created_at | timestamp | |

**Уникальный индекс:** (swiper_id, swiped_id, venue_id, date) — один свайп на пару за вечер.

### Match (PostgreSQL)
| Поле | Тип | Описание |
|------|-----|----------|
| id | UUID | PK |
| user1_id | UUID | FK → User |
| user2_id | UUID | FK → User |
| venue_id | UUID | FK → Venue |
| status | enum | ACTIVE / EXPIRED / MET |
| created_at | timestamp | |
| expires_at | timestamp | Мэтч живёт до конца вечера |

### ConversationTopic (MongoDB) — каталог тем
```json
{
  "_id": "ObjectId",
  "category": "icebreaker | drink | venue | fun_fact",
  "text": "Что сегодня хочется попробовать из меню?",
  "tags": ["cocktail", "bar"],
  "locale": "ru"
}
```

---

## 4. Структура пакетов

```
com.dudev.datingapp
├── config/                  # Spring конфигурация (Security, Redis, Kafka, Mongo, S3)
├── security/                # JWT filter, OAuth2 config, UserDetails
├── common/                  # BaseEntity, исключения, DTO-утилиты
│
├── user/
│   ├── entity/              # User, Photo
│   ├── repository/          # UserRepository, PhotoRepository
│   ├── service/             # UserService
│   ├── dto/                 # UserProfileDto, PhotoUploadDto
│   └── controller/          # UserController (/api/v1/users)
│
├── venue/
│   ├── entity/              # Venue
│   ├── repository/          # VenueRepository
│   ├── service/             # VenueService
│   ├── dto/                 # VenueDto, VenueSearchDto
│   └── controller/          # VenueController (/api/v1/venues)
│
├── plan/
│   ├── entity/              # EveningPlan, EveningPlanTopic
│   ├── repository/          # EveningPlanRepository
│   ├── service/             # PlanService
│   ├── dto/                 # CreatePlanDto (venue_id, date, drink_tonight, topic_ids)
│   └── controller/          # PlanController (/api/v1/plans)
│
├── discovery/
│   ├── service/             # DiscoveryService (Redis Geo + фильтрация)
│   ├── dto/                 # DiscoveryCardDto (фото + topic tags — без имени и возраста)
│   └── controller/          # DiscoveryController (/api/v1/discover)
│
├── swipe/
│   ├── entity/              # Swipe
│   ├── repository/          # SwipeRepository
│   ├── service/             # SwipeService
│   ├── dto/                 # SwipeDto
│   ├── event/               # SwipeEvent (Kafka producer)
│   └── controller/          # SwipeController (/api/v1/swipes)
│
├── match/
│   ├── entity/              # Match
│   ├── repository/          # MatchRepository
│   ├── service/             # MatchService
│   ├── dto/                 # MatchDto, MatchDetailDto (drink_tonight + topics партнёра)
│   ├── consumer/            # SwipeEventConsumer (Kafka → проверка мэтча)
│   └── controller/          # MatchController (/api/v1/matches)
│
├── topic/
│   ├── document/            # ConversationTopic (MongoDB)
│   ├── repository/          # TopicRepository (MongoRepository)
│   ├── service/             # TopicService
│   ├── dto/                 # TopicDto
│   └── controller/          # TopicController (/api/v1/topics)
│
└── notification/
    ├── event/               # MatchEvent, NotificationEvent
    ├── consumer/            # NotificationConsumer (Kafka)
    └── service/             # NotificationService (push / email)
```

---

## 5. API Endpoints

### Auth
| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/v1/auth/register` | Регистрация (телефон + SMS код) |
| POST | `/api/v1/auth/login` | Вход → JWT |
| POST | `/api/v1/auth/refresh` | Обновление токена |
| POST | `/api/v1/auth/oauth2/{provider}` | Вход через Google/Apple |

### User Profile
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/v1/users/me` | Свой профиль |
| PUT | `/api/v1/users/me` | Обновить профиль |
| POST | `/api/v1/users/me/photos` | Загрузить фото |
| DELETE | `/api/v1/users/me/photos/{id}` | Удалить фото |

### Venues
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/v1/venues?area={area}&lat={lat}&lng={lng}` | Найти бары по району / координатам |
| GET | `/api/v1/venues/{id}` | Детали бара |

### Topics (каталог тем)
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/v1/topics?category={category}` | Список тем для выбора при создании плана |

### Evening Plans
| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/v1/plans` | Создать план: venue_id, date, drink_tonight, topic_ids |
| GET | `/api/v1/plans?date={date}` | Мои планы на дату |
| DELETE | `/api/v1/plans/{id}` | Отменить план |

### Discovery
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/v1/discover?venue_id={id}&date={date}` | Карточки людей в том же баре — фото + topic tags, без имени/возраста |

### Swipes
| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/v1/swipes` | Свайпнуть (body: `{swiped_id, direction, venue_id}`) |

### Matches
| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/api/v1/matches` | Мои мэтчи |
| GET | `/api/v1/matches/{id}` | Детали мэтча: drink_tonight партнёра + его темы + заведение |
| PATCH | `/api/v1/matches/{id}/status` | Отметить мэтч как MET |

---

## 6. Основные Flow

### Flow 1: Создание плана и дискавери
```
User → POST /plans {venue_id, date, drink_tonight, topic_ids}
     → PG: сохранить EveningPlan + EveningPlanTopic
     → Redis GEOADD user:{id} lat lng (с TTL до конца вечера)
     → GET /discover?venue_id=X&date=today
     → Redis GEORADIUS → список user_ids рядом
     → PG: отфильтровать уже свайпнутых
     → MongoDB: подтянуть topic names по topic_ids
     → Вернуть карточки: {photo, topic_tags} — без имени и возраста
```

### Flow 2: Свайп → Мэтч
```
User → POST /swipes {swiped_id: Y, direction: LIKE}
     → PG: сохранить Swipe
     → Redis SET: добавить Y в уже-свайпнутые
     → Kafka: publish SwipeEvent
     → Consumer: проверить есть ли обратный LIKE от Y
     → Если да:
        → PG: создать Match
        → Kafka: publish MatchEvent
        → Push-уведомление обоим
```

### Flow 3: После мэтча — подготовка к встрече
```
User → GET /matches/{id}
     → PG: Match + EveningPlan партнёра (drink_tonight + topic_ids) + Venue
     → MongoDB: TopicService.findByIds(topic_ids) → полные тексты тем
     → Ответ: {drink_tonight, topics, venue} — имя партнёра НЕ раскрывается
     → Пользователь идёт в бар и знакомится вживую
```

---

## 7. Ключевые архитектурные решения

**Почему нет чата?**
Концепция приложения — убрать тревогу перед живым знакомством, а не заменить его перепиской. Чат до встречи противоречит идее. После матча пользователь получает только «icebreaker»: что пьёт партнёр сегодня и о чём хочет поговорить — достаточно, чтобы подойти в баре.

**Почему drink_tonight в EveningPlan, а не drink_preference в User?**
Это контекстная информация на конкретный вечер, а не статическое предпочтение. «Сегодня я пью негрони» — живее и точнее, чем «обычно я пью виски».

**Почему темы выбираются пользователем, а не генерируются автоматически?**
Пользователь сам сигнализирует, что ему интересно сегодня. Это делает icebreaker искренним, а не алгоритмическим. MongoDB хранит каталог — пользователь выбирает 2–3 темы при создании плана.

**Почему Kafka, а не просто синхронная проверка мэтча?**
Свайп — самая частая операция. Отделение записи свайпа от проверки мэтча снижает латентность POST /swipes (ответ мгновенный), позволяет масштабировать consumer-ы отдельно, и даёт надёжную доставку уведомлений.

**Почему Redis для geo, а не PostGIS?**
Для этой задачи нужен быстрый in-memory поиск «кто сейчас рядом» с TTL (человек ушёл из бара → ключ истёк). Redis GEORADIUS + TTL — проще и быстрее, чем PostGIS для real-time данных.

**Время жизни мэтча.**
Мэтч экспайрится в конце вечера (expires_at). Это создаёт urgency и соответствует концепции «встретиться сегодня вечером».

**Monolith-first, microservices-ready.**
Пакетная структура уже отражает доменные границы. Kafka events делают межмодульное общение асинхронным. При необходимости модули `notification`, `discovery` можно вынести первыми.
