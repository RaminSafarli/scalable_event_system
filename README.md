# 🚀 Elastic Ticket System

This project is an asynchronous and resilient ticket sales system simulation, designed to manage high-traffic burst requests.

## 🎯 Project Goal

Imagine the moment a concert ticket goes on sale. Thousands of users click the "Buy Ticket" button in the same second.

- **Bad Design (Synchronous):** The API receives each request, connects to the database, checks payment, and makes the user wait. After the first 100 requests, the database locks up, and the system crashes.
- **Good Design (Asynchronous - This Project):** The API _instantly_ accepts every request (HTTP 202 - Accepted), tells the user "Your request has been received," and posts this job request to a Message Queue (RabbitMQ). In the background, "Workers" pull jobs from the queue at their own pace (e.g., 1 per second) and process them calmly.

This project simulates the "Good Design" using multiple microservices orchestrated by Docker Compose and load-tested with k6.

## 🏛️ Architecture

The system consists of 4 decoupled main services:

`\[User\] -> \[POST /ticket\] -> \[1. api-gateway\] -> \[2. rabbitmq\] -> \[3. processor\] -> \[4. postgres\]`

- **api-gateway (Spring Boot):**
    - Handles incoming HTTP POST requests from the user.
    - _Never_ connects to the database or performs slow operations.
    - Directly publishes the incoming request (JSON) to the rabbitmq queue.
    - Instantly returns an HTTP 202 Accepted response to the user.
- **rabbitmq (Message Broker):**
    - Acts as a buffer, collecting thousands of incoming requests from the api-gateway in the ticket_requests queue.
- **processor (Java Worker):**
    - Continuously listens to the ticket_requests queue.
    - When it receives a message, it waits for 1 second to simulate slow work (like payment processing or ticket generation).
    - After the work is "done," it writes the ticket data to the postgres database.
    - It uses _manual acknowledgments_ (autoAck=false) to ensure no data is lost if the worker crashes.
- **postgres (Database):**
    - Provides persistent storage for the tickets processed by the processor.
    - The tickets table is automatically created on startup via a script in docker-entrypoint-initdb.d.

## 🛠️ Tech Stack

- **Java 17**
- **Spring Boot** (for the api-gateway service)
- **PostgreSQL** (Database)
- **RabbitMQ** (Message Queue)
- **Docker & Docker Compose** (Containerization & Orchestration)
- **Maven** (Dependency Management)
- **k6** (Load Testing Tool)

## ✨ Key Concepts Demonstrated

This project demonstrates foundational pillars of modern system design, going far beyond a "simple CRUD" application:

- **Asynchronous Processing:** Managing load and high throughput by not making the user wait.
- **Resilience:** Even if the `processor` crashes, no ticket requests are lost because `autoAck=false` ensures unprocessed messages remain in the queue.
- **Separation of Concerns (SoC):** Each service has one job (API, Worker, Queue, Database).
- **Scalability:** If the load increases, we can scale the workers by running `docker-compose scale processor=10` without changing any code.
- **Configuration Management:** All secrets (passwords, hostnames) are managed externally via an `.env` file.
- **Service Readiness:** Using `healthcheck` or `depends_on` to ensure services start in the correct order (`processor` waits for `postgres`).

## 🚀 How to Run

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [k6 Load Testing Tool](https://grafana.com/docs/k6/latest/set-up/install-k6/)

### 1\. Clone the Project

```
git clone https://github.com/RaminSafarli/scalable_event_system.git

cd scalable_event_system
```

### 2\. Create the Configuration File

This project uses an `.env` file for all passwords and settings. Create a file named `.env` in the same directory as `docker-compose.yml`.

You can copy the example file to get started:

```
\# Linux / macOS  
cp .env.example .env 

\# Windows  
copy .env.example .env\
```

You don't need to edit the `.env` file; the default settings will work.

### 3\. Start the System

To build and launch all services:

`docker-compose up --build`

It may take a moment for the `postgres` and `rabbitmq` services to start. The system is ready when the `processor` service logs show `[✓] Database connection established.` and `[✓] RabbitMQ connection established.`.

## 🧪 How to Test (The Proof)

While the system is running, open a **new terminal** and run the `k6` load test:

`k6 run test.js`

This script will send hundreds of requests to your `api-gateway` over 50 seconds.

### 📊 Observe the Results

While the test is running, watch these 3 things:

- The k6 Terminal:  
  You will see how fast the `api-gateway` is responding (the `http_req_duration` should be in milliseconds) and that all requests are returning `HTTP 202` (the `checks... rate==1.0` will confirm this).
- **RabbitMQ Management UI ( `<http://localhost:15672>` )**
    - Login with: `guest` / `guest`
    - Click the "Queues" tab and find the `ticket_requests` queue.
    - You will see the number in the "Ready" column **skyrocket** (as requests pile up) while the "Unacked" (Unacknowledged) column stays at 1.
    - When the test finishes, watch the "Ready" count **steadily drain** (1 per second, due to our `Thread.sleep(1000))` as the `processor` calmly works through the backlog.
- The Processor Logs:  
  In your `docker-compose` terminal, you will see the `processor-1` service calmly logging one message (`[x] Inserted '...'`) every second.

This is the proof that your system did not crash under heavy load, but successfully buffered and processed every single request.