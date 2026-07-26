#!/bin/bash
# Auto-run by LocalStack on container startup (mounted into /etc/localstack/init/ready.d/).
# Creates the SNS topic, 3 SQS queues (+ their DLQs), and subscribes each queue to the topic —
# reproducing the exact fan-out setup described in the root README, entirely locally.

set -e

REGION="ap-south-1"
ENDPOINT="http://localhost:4566"

echo "Creating SNS topic: order-events"
TOPIC_ARN=$(awslocal sns create-topic --name order-events --region $REGION --query 'TopicArn' --output text)
echo "Topic ARN: $TOPIC_ARN"

create_queue_with_dlq() {
  QUEUE_NAME=$1
  DLQ_NAME="${QUEUE_NAME}-dlq"

  echo "Creating DLQ: $DLQ_NAME"
  DLQ_URL=$(awslocal sqs create-queue --queue-name "$DLQ_NAME" --region $REGION --query 'QueueUrl' --output text)
  DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url "$DLQ_URL" --attribute-names QueueArn --region $REGION --query 'Attributes.QueueArn' --output text)

  echo "Creating queue: $QUEUE_NAME (redrive to $DLQ_NAME after 3 failed attempts)"
  REDRIVE_POLICY="{\"deadLetterTargetArn\":\"$DLQ_ARN\",\"maxReceiveCount\":\"3\"}"
  QUEUE_URL=$(awslocal sqs create-queue --queue-name "$QUEUE_NAME" --region $REGION \
    --attributes "{\"RedrivePolicy\":\"$(echo $REDRIVE_POLICY | sed 's/"/\\"/g')\"}" \
    --query 'QueueUrl' --output text)
  QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$QUEUE_URL" --attribute-names QueueArn --region $REGION --query 'Attributes.QueueArn' --output text)

  echo "Allowing SNS topic to send to queue $QUEUE_NAME"
  POLICY="{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"sqs:SendMessage\",\"Resource\":\"$QUEUE_ARN\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"$TOPIC_ARN\"}}}]}"
  awslocal sqs set-queue-attributes --queue-url "$QUEUE_URL" --attributes "{\"Policy\":\"$(echo $POLICY | sed 's/"/\\"/g')\"}" --region $REGION

  echo "Subscribing $QUEUE_NAME to order-events topic"
  awslocal sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs --notification-endpoint "$QUEUE_ARN" --region $REGION --attributes '{"RawMessageDelivery":"true"}'
  
  echo "Done: $QUEUE_NAME -> $QUEUE_URL"
}

create_queue_with_dlq "notification-queue"
create_queue_with_dlq "inventory-queue"
create_queue_with_dlq "analytics-queue"

echo "LocalStack SNS/SQS setup complete."
