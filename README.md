Seat Booking Infrastructure
📌 Overview

This repository contains Docker configuration for running the complete Seat Booking System locally.

It provisions:

PostgreSQL

Redis

RabbitMQ (with management UI)

🛠 Services

Postgres (Port 5432)

Redis (Port 6379)

RabbitMQ (Ports 5672, 15672)

▶ Start Infrastructure
docker compose up -d

📌 Access

RabbitMQ UI:

http://localhost:15672
