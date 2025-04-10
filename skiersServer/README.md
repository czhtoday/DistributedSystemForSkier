# Assignment2: SkierServer - Scalable Lift Ride Event Processing Server

## Author
Zhihang Cheng

## Overview
The **SkierServer** is a Java-based backend server designed to handle skier lift ride events efficiently. It processes incoming HTTP requests, validates data, and forwards messages to a **RabbitMQ queue** for asynchronous processing. The server is designed to be **scalable and resilient**, supporting **load balancing across multiple instances**.

## How It Works
1. **Handles HTTP Requests**:
   - Accepts **POST** requests with skier lift ride event data.
   - Implements strict **URL path validation** and **JSON payload validation**.
   - Returns appropriate **HTTP status codes** for different validation cases.

2. **Message Queue Integration**:
   - Sends validated lift ride events to a **RabbitMQ queue** for further processing.
   - Uses `MessageQueueProducer` to serialize and publish messages asynchronously.
   - Decouples request handling from database writes, improving performance.

3. **Scalability & Deployment**:
   - Can be deployed on multiple EC2 instances.
   - Uses an **AWS Elastic Load Balancer (ELB)** to distribute traffic across instances.
   - Designed to support **horizontal scaling** with minimal configuration.

## System Components
- **HealthCheckServlet**: Provides a `/healthcheck` endpoint to monitor server health.
- **SkierServlet**: Main API endpoint for handling lift ride event submissions.
- **LiftRide**: Model class representing skier lift ride event data.
- **MessageQueueProducer**: Connects to RabbitMQ and publishes lift ride event messages.

## Deployment Architecture
- **Multiple EC2 Instances**: Each running an instance of SkierServer.
- **AWS Elastic Load Balancer (ELB)**: Distributes incoming requests across instances.
- **RabbitMQ Instance**: Centralized message broker for handling lift ride event processing.
- **ConsumerService**: Reads and processes messages from the queue.

## Achievements
- Successfully processed **200,000 requests** with **zero failures**.
- Peak throughput of **over 12,000 requests per second**.
- Achieved **low request latency** with optimized message handling.
- Fully validated **URL parameters and request payloads** to prevent bad data entry.

This system ensures efficient, **fault-tolerant**, and **scalable** lift ride event processing while maintaining high availability.

