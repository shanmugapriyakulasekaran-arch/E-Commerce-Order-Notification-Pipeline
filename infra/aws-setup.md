# Deploying to Real AWS

This project runs entirely on LocalStack for local development (see root README). This guide
covers standing up the real AWS resources when you're ready to deploy for real, plus the
least-privilege IAM policies referenced in the architecture design decisions.

## 1. Create the SNS topic

```bash
aws sns create-topic --name order-events --region ap-south-1
# Note the TopicArn from the output — you'll need it for order-service's config
```

## 2. Create the SQS queues (with dead-letter queues)

Repeat for each of: `notification-queue`, `inventory-queue`, `analytics-queue`

```bash
# Create the DLQ first
aws sqs create-queue --queue-name notification-queue-dlq --region ap-south-1
# Note its QueueUrl, then get its ARN:
aws sqs get-queue-attributes --queue-url <dlq-queue-url> --attribute-names QueueArn

# Create the main queue with a redrive policy pointing at the DLQ
aws sqs create-queue --queue-name notification-queue --region ap-south-1 \
  --attributes '{"RedrivePolicy":"{\"deadLetterTargetArn\":\"<dlq-arn>\",\"maxReceiveCount\":\"3\"}"}'
```

## 3. Subscribe each queue to the SNS topic

```bash
aws sns subscribe \
  --topic-arn <topic-arn> \
  --protocol sqs \
  --notification-endpoint <queue-arn> \
  --region ap-south-1
```

You must also attach a resource policy to each queue allowing the SNS topic to publish to it
(SNS → SQS delivery requires this explicitly). See `infra/localstack-init.sh` for the exact
JSON policy shape used locally — the same structure applies against real AWS.

## 4. IAM least-privilege policies (referenced in root README)

**Order Service** — can publish, cannot read any queue:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "sns:Publish",
      "Resource": "arn:aws:sns:ap-south-1:<account-id>:order-events"
    }
  ]
}
```

**Each Consumer** — can only read/delete from its own queue:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:ap-south-1:<account-id>:notification-queue"
    }
  ]
}
```
(Swap the Resource ARN per consumer — inventory-consumer only gets `inventory-queue`, etc.
Notice the Inventory Consumer's policy does NOT grant it access to the notification or
analytics queues, even though all three exist in the same AWS account — this is the
"least privilege per service" principle from the README made concrete.)

## 5. Environment variables per service

Set these when running each service against real AWS instead of LocalStack:

```bash
# order-service
AWS_REGION=ap-south-1
ORDER_EVENTS_TOPIC_ARN=arn:aws:sns:ap-south-1:<account-id>:order-events
# remove AWS_SNS_ENDPOINT override entirely so the SDK talks to real AWS

# each consumer
AWS_REGION=ap-south-1
NOTIFICATION_QUEUE_NAME=notification-queue   # (or INVENTORY_/ANALYTICS_ as appropriate)
# remove AWS_SQS_ENDPOINT override entirely
```

Credentials should come from an IAM role (if deploying to ECS/EC2) rather than static keys —
static keys shown in application.yml defaults are for LocalStack only and should never be used
against real AWS.

## 6. RDS instead of local Postgres

Point each service's `spring.datasource.url` at your RDS endpoint, and ensure the RDS security
group allows inbound connections from wherever the services run (ECS tasks, EC2, etc.).

## Cost note

SNS/SQS pricing is pay-per-request and effectively free at demo-project volume (well within
AWS Free Tier's 1 million SNS/SQS requests per month). RDS is the main cost driver if left
running — consider `db.t3.micro` (Free Tier eligible) and stopping it when not actively demoing.
