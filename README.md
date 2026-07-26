# E-Commerce Order Notification Pipeline (Event-Driven, AWS SNS/SQS Fan-Out)

A Spring Boot event-driven system demonstrating the **fan-out messaging pattern** using AWS SNS + SQS.
Built as an extension of my professional experience building e-commerce backend systems (Global Kitchen
project), going deeper into cloud-native, loosely-coupled service architecture.

## Architecture

```
                        ┌─────────────────┐
   POST /orders  ─────▶ │  Order Service    │
                        │  (Spring Boot)    │
                        └─────────┬─────────┘
                                  │ publishes OrderEvent
                                  ▼
                        ┌─────────────────┐
                        │   SNS Topic      │  order-events
                        │  (fan-out)       │
                        └────┬────┬────┬───┘
                              │    │    │
                 ┌────────────┘    │    └────────────┐
                 ▼                 ▼                 ▼
          ┌───────────┐    ┌──────────────┐   ┌──────────────┐
          │ SQS Queue │    │  SQS Queue    │   │  SQS Queue    │
          │notification│    │  inventory    │   │  analytics    │
          └─────┬─────┘    └──────┬───────┘   └──────┬───────┘
                │                  │                   │
                ▼                  ▼                   ▼
       ┌────────────────┐ ┌─────────────────┐ ┌─────────────────┐
       │  Notification   │ │   Inventory      │ │   Analytics      │
       │  Consumer       │ │   Consumer       │ │   Consumer       │
       │  (logs simulated│ │  (decrements     │ │  (writes event   │
       │   email send)   │ │   stock in DB)   │ │   counts to DB)  │
       └─────────────────┘ └─────────────────┘ └─────────────────┘
```

## Why fan-out (SNS → multiple SQS), not a single queue

This is the core architectural decision this project demonstrates, and the one I'd walk an
interviewer through:

- **A single queue with multiple consumers would compete for the same messages** — only one
  consumer processes each message (that's the point of a queue). But here, *all three* downstream
  services need to react to the *same* order event independently.
- **SNS fan-out solves this**: SNS publishes one message to the topic, and it's delivered to
  *every* subscribed SQS queue. Each service then has its own private queue and processes at its
  own pace, with its own retry/DLQ behavior, without any service blocking or waiting on another.
- **Loose coupling**: the Order Service has zero knowledge of notification, inventory, or
  analytics logic. It publishes one event and is done. New consumers (e.g. a future fraud-detection
  service) can subscribe to the same SNS topic later with zero changes to Order Service.
- **Independent failure domains**: if the Analytics Consumer is down or slow, orders still get
  placed, notifications still go out, and inventory still updates — each queue buffers independently.

## Services in this repo

| Service                | Role                                                          | Port |
|-------------------------|----------------------------------------------------------------|------|
| `order-service`         | REST API — place/cancel/update orders, publishes to SNS       | 8080 |
| `notification-consumer` | Consumes `notification` queue, logs simulated email/SMS sends | 8081 |
| `inventory-consumer`    | Consumes `inventory` queue, decrements stock in PostgreSQL     | 8082 |
| `analytics-consumer`    | Consumes `analytics` queue, writes event counts to summary table | 8083 |

Each is an **independently deployable** Spring Boot application — deliberately not a single
monolith with internal method calls, to reflect how this would actually be split across
services/teams in production.

## Tech Stack

- Java 17, Spring Boot 3
- Spring Cloud AWS (SNS/SQS integration)
- PostgreSQL (order data, inventory, analytics summary)
- Spring Data JPA
- Docker Compose + LocalStack (local AWS emulation — no real AWS account needed for local dev)
- JUnit 5, Mockito
- GitHub Actions CI

## Running locally (LocalStack — no AWS account or cost required)

```bash
docker compose up -d
```

This starts:
- PostgreSQL
- LocalStack (emulates SNS, SQS locally)
- The `infra/localstack-init.sh` script auto-creates the SNS topic and 3 SQS queues with subscriptions on container startup

Then run each service:
```bash
cd order-service && mvn spring-boot:run
cd notification-consumer && mvn spring-boot:run
cd inventory-consumer && mvn spring-boot:run
cd analytics-consumer && mvn spring-boot:run
```

Test it:
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-1","productId":"prod-100","quantity":2,"unitPrice":499.00}'
```

Watch the logs of all three consumers — you'll see the same order event processed independently
by each one, arriving at slightly different times, proving the fan-out actually works.

## Deploying to real AWS

See `infra/aws-setup.md` for step-by-step SNS topic + SQS queue + subscription creation via AWS CLI,
plus IAM least-privilege policy examples per service (Order Service only needs `sns:Publish`;
consumers only need `sqs:ReceiveMessage`/`DeleteMessage` on their own queue — not each other's).

## Design Decisions / Trade-offs

- **At-least-once delivery**: SQS guarantees at-least-once, not exactly-once — consumers are
  written to be idempotent (e.g. Inventory Consumer checks if it already decremented stock for a
  given `orderId` before applying the change again) rather than assuming each message arrives once.
- **Dead-letter queues (DLQ)**: each SQS queue is configured with a DLQ after 3 failed processing
  attempts, so a malformed or bug-triggering message doesn't block the whole queue forever.
- **PostgreSQL per consumer vs. shared DB**: for this demo, all consumers share one Postgres
  instance with separate schemas/tables for simplicity. In a real microservices setup I'd use
  separate databases per service to avoid coupling through shared tables — noted here as a known
  simplification, not an oversight.
- **IAM least privilege**: each service's IAM role only has permissions for the specific SNS/SQS
  resource it needs — Order Service can publish but not read any queue; each consumer can read
  only its own queue.

## What I'd add next

- CloudWatch alarm on DLQ message count (alerts if messages are consistently failing)
- API Gateway + Lambda authorizer in front of Order Service instead of open REST endpoint
- Contract tests between Order Service and consumers to catch event schema drift early
