# devgate-core-service

Модульный монолит DevGate: **identity** (auth, users, teams, organizations) + **catalog**
(projects, services, secrets registry) + **scaffolder** (templates, `run`). Объединяет бывшие
`devgate-user-service`, `devgate-catalog-service` и `devgate-scaffolder-service` (их каталоги в
монорепо — legacy-архив, не развивать). Порт **8092**, Postgres **5444** (БД `devgate_core`).
Общие конвенции монорепо — корневой `AGENTS.md`.

## Working with me

- **Language:** все ответы и объяснения — на русском. Код, имена, комментарии — только English.
- **Commits:** на русском, conventional-style (`feat:`, `fix:`, `refactor:`, ...).
- **Plans first:** перед написанием кода используй `EnterPlanMode` и дождись одобрения. См. `.claude/rules/workflow.md`.
- **After code changes:** tests + type-check + ktlint. См. `.claude/rules/checks.md`.
- **Logs:** после каждой задачи создавай `.claude/logs/YYYY-MM-DD-task-name.md` с обязательной секцией `## Summary`.
- **Git push:** всегда спрашивай перед push. Никогда не force-push.

## Stack

Kotlin 2.2.21, JVM toolchain 21, Gradle 9.2.1 (wrapper), Spring Boot 4.0.1 (WebMVC, Data JPA,
Security, Actuator, Validation, RestClient, AMQP), PostgreSQL + Liquibase 5 (YAML), JJWT 0.12
(HS256), springdoc (`/swagger-ui.html`). Тесты: JUnit 5 + Mockito 5 (inline, javaagent) + H2.
ktlint (`ignoreFailures = true` — в унаследованном коде identity есть старые нарушения; новых не добавлять).

## Модули и структура

```
src/main/kotlin/com/devgate/
├── domains/
│   ├── auth/ users/ teams/ organizations/   # identity (бывший user-service)
│   ├── projects/ services/ secrets/         # catalog  (бывший catalog-service)
│   ├── templates/                           # scaffolder (бывший scaffolder-service)
│   └── audit/                               # общий: Action enum, LogMessagePayload, AuditLogService
├── security/      # общий JWT RequestFilter, AuthenticatedUser, 401/403 handlers, JwtTokenValidator
├── config/        # SecurityConfig, AppConfig (UserDetailsService для логина), RabbitMq, OpenApi
├── exceptions/    # ApiException + доменные исключения, GlobalErrorHandler
└── utils/
src/main/resources/db/changelog/
├── baseline/{identity,catalog,scaffolder}/  # НЕИЗМЕНЁННЫЕ changeSet'ы трёх сервисов — не трогать
└── changes/                                 # новые changeSet'ы (сквозная нумерация 001-, 002-, ...)
```

- Внутри домена — прежняя раскладка: `controllers/ dto/ models/ repositories/ services/(impl/)`.
- **Межмодульные вызовы — только через интерфейсы `services/*Service`**, не через чужие
  репозитории/сущности. Пример: `TemplateServiceImpl` → `ServiceCatalogService`.
- Таблицы всех модулей — в одной схеме `public`. `Project.teamId` / `Service.ownerTeamId` по-прежнему
  opaque UUID без FK на `teams` (в данных возможны сироты; FK — отдельная задача с политикой удаления).

## Безопасность

- Токены выпускает сам сервис (`domains/auth/security/TokenGenerator`): access 15 мин (claims
  `sub`=email, `userId`, `role`, `type`), refresh 30 дней в HttpOnly cookie `jwt-refresh-cookie`.
  `JWT_SECRET_BASE64` общий для всех сервисов DevGate.
- `security/RequestFilter`: битый/протухший токен → **401** (фронт делает refresh). Если `sub`
  есть в `users` — id и role **из БД** (смена роли действует сразу), иначе — из claims
  (сервисные токены readme/daily-summary: `sub=*@devgate.local`, role ADMIN).
- `AuthenticatedUser` реализует `java.security.Principal` → `authentication.name` = email
  (на это опирается `UserService.getCurrentUser()`).
- Запись — `@PreAuthorize("hasRole('ADMIN')")`; нехватка роли → **403**. Исключение — self-service
  `POST /templates/{id}/run` (любой authenticated; создаёт Service в процессе, минуя ADMIN-проверку
  `POST /services`, — осознанно).
- Тело запроса десериализуется до `@PreAuthorize`: невалидный JSON даёт 422 раньше, чем 403.

## Template run

`TemplateServiceImpl.runTemplate` **без** `@Transactional` (не держит транзакцию на время HTTP):
шаблон читается в коротком `TransactionTemplate(readOnly)` → `ServiceCatalogService.validateServiceCreation`
(409/404 до создания репозитория) → `IntegrationServiceClient.generateRepoFromTemplate` (HTTP, проброс
`Authorization`; ошибка → 502) → `ServiceCatalogService.createService`. Сирота-репозиторий возможен
только при гонке slug — логируется с `htmlUrl`. Аудит: `service.created` + `template.used` (контроллер).

## Аудит

`AuditLogService.sendMessage(action, target)` → `audit.exchange`/`audit.routingKey`, `actorId` из
principal; best-effort (ошибка AMQP только логируется). **Новое значение `Action` — зеркалить в
`devgate-audit-service/.../models/Action.kt` и деплоить парой.**

## Миграция данных из legacy-БД

`scripts/migrate-legacy-data.sh --k8s|--docker [--dry-run]` — однократный перенос строк из
`users` / `devgate_catalog` / `devgate_scaffolder` в `devgate_core`. Схему создаёт Liquibase при
первом старте сервиса; скрипт: preflight (цель пуста) → `pg_dump --data-only` → одна транзакция
→ сверка строк. k8s: `make -C k8s migrate-core` (полный сценарий с бэкапом и переключением gateway).
Docker: `make up` → `make migrate-data`.

## Development commands

| Задача | Команда |
|--------|---------|
| Запуск | `./gradlew bootRun` (env из `.env`) или `make up` (bootJar + compose: app :8092, postgres :5444) |
| Тесты | `./gradlew test` |
| Полная сборка | `./gradlew build` |
| Type-check | `./gradlew compileKotlin compileTestKotlin` |
| Стиль | `./gradlew ktlintCheck` / `ktlintFormat` (format трогает и унаследованный код — осторожно) |
| Первый админ | `make seed` (docker) / `make -C ../k8s seed` |
| Перенос данных | `make migrate-data` (docker) / `make -C ../k8s migrate-core` |

## Env

`POSTGRES_HOST/PORT/DB/USERNAME/PASSWORD`, `JWT_SECRET_BASE64`, `JWT_COOKIE_SECURE`,
`JWT_COOKIE_SAME_SITE`, `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`, `INTEGRATION_SERVICE_URL`,
`CORS_ALLOWED_ORIGINS`. Пример — `.env.example`.

@.claude/rules/general.md
@.claude/rules/workflow.md
@.claude/rules/orchestration.md
@.claude/rules/checks.md
@.claude/rules/naming.md
@.claude/rules/code-generation.md
