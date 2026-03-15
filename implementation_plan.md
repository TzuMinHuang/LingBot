# Architecture Migration Implementation Plan

This plan addresses the migration of the LingBot architecture to utilize Nginx for SSE, Spring Boot as the core API, Redis as a communication hub (Streams + Pub/Sub), PostgreSQL with pgvector for persistence, and vLLM/AnythingLLM for inference, per your requirements.

## Proposed Changes

### 1. Nginx Configuration
#### [MODIFY] [default.conf](file:///Users/j/web/chatbot/LingBot/nginx/default.conf)
- Update the `/bot/` location block to support Server-Sent Events (SSE).
- Add the required SSE directives:
  - `proxy_buffering off;`
  - `proxy_cache off;`
  - `proxy_http_version 1.1;`
  - `proxy_set_header Connection "";`

---

### 2. Infrastructure (`docker-compose.yml`)
#### [MODIFY] [docker-compose.yml](file:///Users/j/web/chatbot/LingBot/docker-compose.yml)
- **Remove**: `rabbitmq`, `mongodb`, and `mongo-express` services since they are being replaced by Redis Streams and PostgreSQL.
- **Update PostgreSQL**: Change the `postgres` image to `pgvector/pgvector:pg15` to support vector storage and RAG features.
- **Add Inference UI/Engine**: Add `AnythingLLM` (or `vllm`) as a service to manage the document knowledge base and RAG.
- **Update Spring Boot Environment Variables**: Remove `RABBITMQ_*` and `MONGODB_*` references from `bot-service` and `admin-service`.

---

### 3. Spring Boot Backend Core Logic (`backend/bot`)
#### [MODIFY] [application.yml](file:///Users/j/web/chatbot/LingBot/backend/bot/src/main/resources/application.yml)
- Remove `spring.data.mongodb` and `spring.rabbitmq` configurations.
- Ensure Redis is properly configured for the Lettuce client.

#### [MODIFY] [pom.xml](file:///Users/j/web/chatbot/LingBot/backend/bot/pom.xml)
- Ensure `spring-boot-starter-data-redis` is included.
- Ensure RabbitMQ and MongoDB dependencies are removed (check `common` module as well if inherited).

#### [NEW] `ChatStreamController.java`
- Expose `GET /bot/stream` or `POST /bot/stream` producing `text/event-stream`.
- Implement `SseEmitter` logic to hold the connection open indefinitely (`0L`).
- Publish the incoming prompt to a **Redis Stream** (Task Queue).

#### [NEW] `RedisMessageSubscriber.java`
- Implement a Redis Pub/Sub listener that listens for newly generated tokens from the Worker/vLLM.
- Correlate the received tokens by a UUID/SessionId and dispatch the token to the corresponding active `SseEmitter`.
- When an `[DONE]` token is received, call `emitter.complete()`.

#### [NEW] `BotTaskWorker.java` (Optional/If implemented in Spring Boot)
- Consume the Redis Stream task queue.
- Forward the prompt context to AnythingLLM / vLLM.
- As tokens are flushed back from AnythingLLM, publish them immediately to the Redis Pub/Sub channel.

---

## Verification Plan

### Automated/Manual Tests
1. **Infrastructure Validation**: Run `docker-compose config` to ensure the structure is valid, and then `docker-compose up -d --build` to verify that Postgres (pgvector), Redis, and Spring Boot start successfully without RabbitMQ/MongoDB.
2. **Nginx Syntax Check**: Run `nginx -t` inside the Nginx container to verify the syntax optimizations for SSE.
3. **SSE Connection Test**: Send a request to the newly created endpoint (`curl -N -H "Accept: text/event-stream" http://localhost/bot/stream?message=hello`) and verify that responses are chunked and streamed character-by-character as they arrive from Redis.
4. **Backend Application Lift**: Verify that Spring Boot initializes the Redis Stream consumer and Pub/Sub listener successfully by checking application startup logs.
