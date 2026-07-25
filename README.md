#  PhishSim — AI-Assisted Phishing Simulation Platform

> A controlled phishing simulation platform for security awareness training, powered by AI.  
> Built during an internship at **Intellisec Solutions** — July/August 2026.

---

##  Overview
PhishSim allows security teams to create and run realistic phishing campaigns against internal employee groups in a **safe, authorized, and fully instrumented** environment. It tracks open, click, and submission events, and leverages the **Claude API** to auto-generate phishing content from a high-level brief.

> ⚠️ For authorized security awareness training only. Never use against unauthorized recipients.

---

##  Tech Stack

| Layer          | Technology                                           |
|---             |------------------------------------------------------|
| Backend        | Spring Boot 4.1 · Java 21 · Spring Security · Flyway |
| Frontend       | Angular 19 · TypeScript                              |
| Database       | PostgreSQL 15                                        |
| Auth           | BCrypt · JWT · TOTP (Google Authenticator)           |
| Email(dev)     | Mailpit (local SMTP catcher)                         |
| AI             | Claude API (Anthropic)                               |
| Infrastructure | Docker Compose                                       |

---

##  Getting Started

### Prerequisites
- Java 21, Maven, Node.js 20+, Angular CLI 19+, Docker Desktop

### 1. Clone & start infrastructure
```bash
git clone https://github.com/mariembenzakour/phishing-simulator.git
cd phishing-simulator
docker-compose up -d
```

### 2. Start the backend
```bash
cd backend
./mvnw spring-boot:run
```

### 3. Start the frontend
```bash
cd frontend
npm install && ng serve
```

### 4. Access the app

| Service           | URL                                         |
|-------------------|---------------------------------------------|                   
| Frontend          | http://localhost:4200                       |
| Swagger UI        | http://localhost:8086/swagger-ui/index.html |
| Mailpit           | http://localhost:8025                       |

> On first startup, Flyway automatically creates all database tables.

---

##  Roles & Permissions

| Action                      | SUPER_ADMIN | ADMIN | OPERATOR | VIEWER |
|                             |-----------------------------------------              
| Create / Clone campaign     | ✅         | ✅    | ✅       | ❌ |
| Authorize / Delete campaign | ✅         | ✅    | ❌       | ❌ |
| Import targets (CSV)        | ✅         | ✅    | ✅       | ❌ |
| View everything             | ✅         | ✅    | ✅       | ✅ |
| Create OPERATOR account     | ✅         | ✅    | ❌       | ❌ |
| Create / Delete ADMIN       | ✅         | ❌    | ❌       | ❌ |

---

##  Security Guardrails

```
✅ Authorization Gate      → Every campaign requires explicit ADMIN approval
✅ Scope Enforcement       → Pre-send check blocks any out-of-scope recipient
✅ No Credential Harvest   → Submission records the fact only, never the values
✅ Human-in-the-loop AI    → AI-generated content requires human approval before send
✅ Immutable Audit Log     → Append-only — no UPDATE or DELETE on audit records
```

---


API Endpoints

### Authentification

| Méthode | Endpoint                         | Description                       |
|---------|--------------------------------- |
| POST    | `/api/auth/register`             | Register a new operator account
                                                 (force VIEWER role)e            |
| POST    | `/api/auth/login`                | Connexion                         |
| POST    | `/api/auth/mfa/enable`           | Activation MFA                    |
| POST    | `/api/auth/mfa/verify`           | Verificationr MFA                 |
| POST    | `/api/auth/admin/create-operator`| create operateur (ADMIN)          |

### Campagnes

| Méthode                             | Endpoint                        | Description          |
|-------------------------------------|---------------------------------|----------------------|
| GET                                 | `/api/campaigns`                | compaign list        |
| POST                                | `/api/campaigns`                | compaign create      |
| PUT                                 | `/api/campaigns/{id}/authorize` | authorization        |
| PUT                                 | `/api/campaigns/{id}/pause`     | paused               |
| DELETE                              | `/api/campaigns/{id}`           | delete               |

### Email

| Méthode                               | Endpoint                        | Description            |
|---------------------------------------|---------------------------------|------------------------|
| POST                                  | `/api/email/test`               | Send a test email via 
                                                                                SMTP               |
| POST                                  | `/api/email/send-campaign/{id}` | Send a campaign   |




## Structure du Projet

```
phishing-simulator/
├── backend/
│   ├── src/main/java/com/intellisec/phishsim/
│   │   ├── auth/           # Authentication, JWT, MFA
│   │   ├── campaign/       # Campaign management
│   │   ├── target/         # Targets and groups
│   │   ├── email/          # SMTP, SenderProfile, EmailTemplate
│   │   ├── common/         # Configuration, security, JWT
│   │   └── BackendApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/   # Flyway migrations
│   └── pom.xml
├── frontend/
│   ├── src/app/
│   │   ├── auth/           # Login, Register, MFA
│   │   ├── campaigns/      # Campaigns
│   │   ├── groups/         # Groups
│   │   ├── targets/        # Targets
│   │   ├── operators/      # Operator management
│   │   ├── shared/         # Services, guards, interceptors
│   │   └── app.routes.ts
│   ├── package.json
│   └── angular.json
├── docker-compose.yml
└── README.md
---


##  Progress

| Week | Theme                              |
|---   |------------------------------------|                                 
| 1    | Foundations & Architecture         | 
| 2    | Auth, RBAC, Campaign & Target Core | 
| 3    | Email Sending & Test Harness       | 
| 4    | Tracking & Telemetry               | 
| 5    | AI Content Generation              |
| 6    | AI Design & Defensive Flip         | 
| 7    | Reporting & Hardening              | 
| 8    | Capstone & Write-up                | 

---

## Author

**Mariem Ben Zakour** — ESPRIT, 4th Year Software Engineering  
Internship supervisor: **Chiheb Chebbi** — Intellisec Solutions# 🛡️ PhishSim — AI-Assisted Phishing Simulation Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen?logo=springboot)](https://spring.io)
[![Angular](https://img.shields.io/badge/Angular-19-red?logo=angular)](https://angular.io)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)](https://www.docker.com)

> A controlled phishing simulation platform for security awareness training, powered by AI.  
> Built during an internship at **Intellisec Solutions** — July/August 2026.

---

