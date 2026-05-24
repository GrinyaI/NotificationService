# Notification Service

Сервис управляет уведомлениями по каналам `EMAIL`, `SMS` и `PUSH`.
Реальная интеграция с email/SMS/push-провайдерами пока не подключена: доставка сейчас симулируется Kafka-consumer-ами.

## Возможности

- Создание персональных, широковещательных и сегментных уведомлений.
- Приоритеты уведомлений: `LOW`, `NORMAL`, `HIGH`, `URGENT`.
- Обязательное идемпотентное создание через `idempotencyKey`.
- Защита от повторной отправки через outbox claim и идемпотентный consumer.
- Асинхронная обработка через Kafka и outbox-таблицу.
- Хранение в PostgreSQL, кеширование чтения через Redis.
- Автоархивация старых уведомлений по расписанию.

## Защита от дублей

Каждый `POST /api/notifications` должен содержать `idempotencyKey`.
Повтор того же запроса с тем же ключом возвращает уже созданные уведомления.
Повтор того же ключа с другим телом запроса возвращает `409 Conflict`.

На уровне БД ключ защищен уникальным индексом `(idempotency_key, channel)`.
Outbox публикует записи через claim с `FOR UPDATE SKIP LOCKED`, поэтому несколько инстансов приложения не должны брать одну и ту же `PENDING` запись одновременно.
Если приложение упало после claim, зависшая запись `PROCESSING` вернется в `PENDING` после таймаута.
Consumer повторно не обрабатывает уведомление, которое уже находится в статусе `SENT`.

## Пример запроса

```json
{
  "recipientId": "youremail@example.com",
  "audienceType": "PERSONAL",
  "payload": "Hello, world!",
  "priority": "NORMAL",
  "channels": ["EMAIL", "SMS", "PUSH"],
  "channelDestinations": {
    "EMAIL": "youremail@example.com",
    "SMS": "+79990000000",
    "PUSH": "device-token"
  },
  "idempotencyKey": "client-request-123"
}
```

Для широковещательной рассылки используйте `audienceType: "BROADCAST"` без `recipientId`.
Для сегментной рассылки используйте `audienceType: "SEGMENT"` и `audienceTarget`.

## Сборка и проверки

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd package
```

Linux/macOS:

```bash
./mvnw test
./mvnw verify
./mvnw package
```

`test` запускает быстрые unit-тесты.
`verify` дополнительно запускает интеграционные Testcontainers-тесты и требует Docker.

## Docker

Создайте локальный `.env` из [.env.example](.env.example), если нужно переопределить порты или пароли.

```bash
docker compose up -d notification-service
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Остановить стек и удалить локальные volume:

```bash
docker compose down -v
```

## Основные переменные окружения

- `APP_PORT` - внешний порт приложения.
- `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` - настройки PostgreSQL.
- `KAFKA_EXTERNAL_PORT` - внешний порт Kafka для подключения с хоста.
- `REDIS_PORT` - внешний порт Redis.
- `JAVA_OPTS` - дополнительные JVM-опции.
- `NOTIFICATION_ARCHIVE_CRON` - cron автоархивации.
- `NOTIFICATION_ARCHIVE_RETENTION_DAYS` - срок хранения активных уведомлений до архивации.
