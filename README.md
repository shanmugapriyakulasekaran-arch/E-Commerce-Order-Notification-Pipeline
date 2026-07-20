# E-Commerce-Order-Notification-Pipeline
Order service publishes events (order placed, cancelled, shipped) → SNS fans out to multiple SQS queues → separate consumers handle email notification (simulated), inventory update, and analytics logging.
