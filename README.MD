# devgate-core-service

Ядро DevGate — модульный монолит на Kotlin / Spring Boot 4: **identity** (аутентификация,
пользователи, команды, организации), **catalog** (проекты, каталог сервисов, реестр ссылок на
секреты) и **scaffolder** (шаблоны и генерация репозиториев). Заменяет `devgate-user-service`,
`devgate-catalog-service` и `devgate-scaffolder-service`; REST-пути не изменились, поэтому
фронтенд и gateway работают как раньше.

- Порт **8092**, PostgreSQL **5444** (БД `devgate_core`), Swagger UI — `/swagger-ui.html`.
- Выпускает JWT (HS256, `JWT_SECRET_BASE64` общий для всех сервисов): access 15 мин,
  refresh 30 дней в HttpOnly cookie `jwt-refresh-cookie`.
- Аудит — RabbitMQ `audit.exchange` / `audit.routingKey`, best-effort.
- Единственный синхронный внешний вызов — `devgate-integration-service` (генерация репозитория).

## Стек

Kotlin 2.2.21, JVM 21, Gradle 9.2.1 (wrapper), Spring Boot 4.0.1 (WebMVC, Data JPA, Security,
Actuator, Validation, RestClient, AMQP), PostgreSQL + Liquibase 5 (YAML), JJWT 0.12.7,
springdoc 3.0.3. Тесты — JUnit 5 + Mockito 5 + H2; стиль — ktlint 1.7.1 (`ignoreFailures = true`:
в унаследованном коде identity есть старые нарушения).

## Структура

```
src/main/kotlin/com/devgate/
├── domains/
│   ├── auth/ users/ teams/ organizations/   # identity
│   ├── projects/ services/ secrets/         # catalog
│   ├── templates/                           # scaffolder (+ clients/IntegrationServiceClient)
│   └── audit/                               # Action enum, LogMessagePayload, AuditLogService
├── security/      # RequestFilter (JWT), AuthenticatedUser, 401/403 handlers
├── config/        # SecurityConfig, AppConfig, RabbitMqConfig, OpenApiConfig
├── exceptions/    # ApiException, доменные исключения, GlobalErrorHandler
└── utils/
src/main/resources/db/changelog/
├── baseline/{identity,catalog,scaffolder}/  # неизменённые changeSet'ы трёх сервисов
└── changes/                                 # новые changeSet'ы (пока пусто)
```

Внутри домена: `controllers/ dto/ models/ repositories/ services/(impl/)`. Межмодульные
вызовы идут только через интерфейсы `services/*Service`, не через чужие репозитории
(пример: `TemplateServiceImpl` → `ServiceCatalogService`).

## Модель данных

Все таблицы — в схеме `public`; `ddl-auto: validate`, схему ведёт Liquibase.

| Модуль | Таблицы |
|---|---|
| identity | `users`, `refresh_tokens`, `teams`, `team_members`, `organizations`, `departments` |
| catalog | `projects`, `services` (+ `service_tags`, `service_dependencies`, `service_environments`), `secrets` |
| scaffolder | `templates` (+ `template_tags`) |

- Роли пользователя: `MEMBER`, `ADMIN`, `MANAGER`, `DEVOPS`, `QA`; роль в команде: `OWNER`,
  `MAINTAINER`, `MEMBER`.
- `Project.teamId` и `Service.ownerTeamId` — opaque UUID без FK на `teams`: в данных возможны
  сироты (FK требует политики удаления команды).
- `Template.integrationId` — opaque UUID сущности `devgate-integration-service`, существование не
  проверяется.
- `Service`: `health` (`healthy|degraded|down|unknown`), окружения `dev|stage|prod` с `url`,
  `dependencies` — плоский список id, SLO — два nullable-поля (`sloAvailability`,
  `sloLatencyP95Ms`).
- `Secret` хранит только **ссылку** (`provider` + `reference`), провайдеры: `vault`,
  `aws_secrets_manager`, `azure_key_vault`, `gcp_secret_manager`, `other`. `rotate` лишь
  выставляет `lastRotatedAt` — значение секрета сервис не хранит и не меняет.

## API

`[ADMIN]` — `@PreAuthorize("hasRole('ADMIN')")`; остальное — любой authenticated, кроме публичных
`/auth/*`. Ошибки: битый/протухший токен → 401, нехватка роли → 403, невалидный UUID в пути → 400,
дубль `slug` → 409, нет сущности → 404.

| Метод | Путь | Доступ |
|---|---|---|
| GET | `/auth/need-register` | public |
| POST | `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout` | public (`register` — только пока нет пользователей, первый — ADMIN) |
| GET | `/auth/me`, `/users/current` | authenticated |
| GET | `/users`, `/users/{id}` | authenticated |
| POST / PUT | `/users` | ADMIN |
| PATCH | `/users/{id}/role` | ADMIN |
| DELETE | `/users/{id}` | ADMIN |
| GET | `/teams`, `/teams/{id}`, `/teams/{id}/members` | authenticated |
| POST / PATCH / DELETE | `/teams`, `/teams/{id}` | ADMIN |
| POST | `/teams/{id}/members` | ADMIN |
| PATCH | `/teams/{id}/members/{memberId}/role` | ADMIN |
| DELETE | `/teams/{id}/members/{memberId}` | ADMIN |
| GET | `/organizations`, `/organizations/{id}` | authenticated |
| POST | `/organizations` (с деревом отделов, глубина ≤ 3) | ADMIN |
| POST | `/organizations/{id}/departments` | ADMIN |
| PATCH / DELETE | `/organizations/{id}/departments/{departmentId}` | ADMIN |
| GET | `/projects`, `/projects/{id}` | authenticated |
| POST / PATCH / DELETE | `/projects`, `/projects/{id}` | ADMIN |
| GET | `/services` (`?search&tags&health&ownerTeamId&projectId`), `/services/tags`, `/services/{id}` | authenticated |
| POST / PATCH / DELETE | `/services`, `/services/{id}` | ADMIN |
| GET | `/services/{serviceId}/secrets[/{id}]` | authenticated |
| POST / PATCH / DELETE | `/services/{serviceId}/secrets[/{id}]`, `POST .../{id}/rotate` | ADMIN |
| GET | `/templates` (`?search`), `/templates/{id}` | authenticated |
| POST / PATCH / DELETE | `/templates`, `/templates/{id}` | ADMIN |
| POST | `/templates/{id}/run` | authenticated (self-service) |
| GET | `/swagger-ui.html`, `/v3/api-docs`, `/actuator/health` | public |

Списки фильтруются в памяти, без пагинации.

### `POST /templates/{id}/run`

Тело: `newRepoName` (обязательно), `ownerTeamId`, `projectId`, `private` (по умолчанию `true`),
`description`.

1. Проверяет, что сервис с `slug = newRepoName` ещё не существует и `projectId` есть (409 / 404 —
   до создания репозитория).
2. Генерирует репозиторий через `devgate-integration-service` (`Authorization` пробрасывается как
   есть; недоступен/ошибка → 502).
3. Создаёт Service в каталоге (в процессе, одной транзакцией) и публикует `service.created` +
   `template.used`. Ответ: `{ "service": ServiceResponse }`.

Транзакция на время HTTP-вызова не держится. Сирота-репозиторий возможен только при гонке slug.

## Безопасность

- `security/RequestFilter` валидирует access-токен (claims `sub` = email, `userId`, `role`).
  Если пользователь есть в `users`, id и роль берутся **из БД** (смена роли действует сразу),
  иначе — из claims (сервисные токены readme / daily-summary: `sub=*@devgate.local`, роль ADMIN).
- Сессии stateless, CSRF отключён, CORS: `allowCredentials`, origins из `CORS_ALLOWED_ORIGINS`.
- `POST /auth/register` открыт, только пока в `users` пусто; первый пользователь всегда `ADMIN`
  (advisory lock + транзакция). Остальных создаёт ADMIN через `POST /users`.
- Тело запроса десериализуется до `@PreAuthorize`: невалидный JSON даёт 422 раньше, чем 403.

## Аудит

`AuditLogService.sendMessage(action, target)` публикует в `audit.exchange` / `audit.routingKey`;
ошибка AMQP только логируется, запрос не падает. Действия (`domains/audit/models/Action.kt`):

| Модуль | Действия |
|---|---|
| users | `user.created`, `user.updated`, `user.deleted`, `user.role_changed` |
| teams | `team.created`, `team.updated`, `team.deleted`, `team.member.added`, `team.member.updated`, `team.member.removed` |
| organizations | `organization.created`, `department.created`, `department.updated`, `department.deleted` |
| projects | `project.created`, `project.updated`, `project.deleted` |
| services | `service.created`, `service.updated`, `service.deleted` |
| secrets | `secret.created`, `secret.updated`, `secret.deleted`, `secret.rotated` |
| templates | `template.used` |

> `devgate-audit-service` десериализует `action` строго в свой enum. Новое значение `Action`
> нужно зеркалить в `devgate-audit-service/.../models/Action.kt` и деплоить парой.

## Конфигурация

Пример — `.env.example`.

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD` | — (обязательны) | подключение к БД; в docker `POSTGRES_HOST=devgate-core-db`, порт `5432` |
| `JWT_SECRET_BASE64` | — (обязателен) | HS256-секрет, общий для всех сервисов |
| `JWT_COOKIE_SECURE` / `JWT_COOKIE_SAME_SITE` | `false` / `Lax` | атрибуты refresh-cookie |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | `localhost`, `5672`, `guest`, `guest` | брокер для аудита |
| `INTEGRATION_SERVICE_URL` | `http://devgate-integration-service:8083` | генерация репозитория при `template run` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | разрешённые origins |

## Запуск

```bash
cp .env.example .env          # заполнить JWT_SECRET_BASE64 (тот же, что у остальных сервисов)
docker network create app_network   # один раз
make up                       # bootJar + docker compose (app :8092, postgres :5444)
make seed                     # (опц.) первый администратор
```

| Команда | Что делает |
|---|---|
| `make up` | `down` → `./gradlew bootJar` → `docker compose up --build -d` |
| `make start` | `down` → `docker compose up -d` без пересборки jar |
| `make down` | остановка контейнеров |
| `make seed` | `docker/seed-admin.sh`: админ напрямую в БД (BCrypt, идемпотентно); `ADMIN_EMAIL` (по умолчанию `admin@devgate.local`), `ADMIN_NAME`, `ADMIN_PASSWORD` (иначе спросит) |
| `make migrate-data` | перенос данных из legacy-БД (см. ниже) |
| `./gradlew bootRun` | запуск без Docker: переменные из `.env` нужно экспортировать в shell, `POSTGRES_HOST=localhost`, `POSTGRES_PORT=5444`, `RABBITMQ_HOST=localhost` |

Проверка: `curl localhost:8092/actuator/health` и `curl localhost:8092/auth/need-register`.

## Тесты и проверки

```bash
./gradlew test            # JUnit 5 + Mockito, H2 вместо Postgres
./gradlew ktlintCheck     # стиль по .editorconfig (ktlintFormat трогает и унаследованный код)
./gradlew build           # тесты + ktlint; то же гоняет .githooks/pre-push
```

Покрыты сервисы auth, token manager, users, teams, organizations, secrets, templates, а также
`RequestFilter`; `ApplicationTest` поднимает контекст. В тестах Liquibase выключен
(`ddl-auto: create-drop` на H2), RabbitMQ-слушатели не стартуют.

## Перенос данных из user/catalog/scaffolder

Схема `devgate_core` = неизменённые changeSet'ы трёх сервисов (`db/changelog/baseline/*`), поэтому
данные переносятся как есть, без преобразований:

```bash
# Kubernetes (все БД в postgres-0): бэкап → devgate_core → core без трафика → стоп старых →
# перенос → gateway на core → удаление старых deployments
make -C ../k8s migrate-core

# docker-compose (каждая БД в своём контейнере): core уже поднят `make up`
./scripts/migrate-legacy-data.sh --docker --dry-run
make migrate-data
```

`scripts/migrate-legacy-data.sh`:
- **Preflight.** Отказывается работать, если в `devgate_core` есть хоть одна строка. Это защищает от
  повторного запуска и от регистрации на новом сервисе до переноса.
- **Перенос.** `pg_dump --data-only` трёх БД, затем одна транзакция `psql --single-transaction`,
  FK включены: либо всё, либо ничего.
- **Сверка.** Число строк по каждой таблице «источник = цель».

Старые БД только читаются. Имена контейнеров и БД переопределяются через env (см. `--help`).
