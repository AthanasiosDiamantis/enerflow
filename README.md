# ⚡ EnerFlow – Intelligent Energy Management System

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.15-brightgreen?logo=springboot)
![Maven](https://img.shields.io/badge/Maven-3.x-red?logo=apachemaven)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Tests](https://img.shields.io/badge/Tests-80%20passing-brightgreen)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Status](https://img.shields.io/badge/Status-Feature%20Complete-success)

> **Portfolio Project** — developed as part of a Java qualification program (May–July 2026)

EnerFlow is a local energy management system that intelligently connects a photovoltaic (PV)
system, a battery storage unit, and a heat pump. Excess solar energy is automatically converted
into thermal energy by dynamically raising the heat pump's hot water target temperature —
instead of feeding unused electricity back into the grid.

---

## Table of Contents

- [Core Logic](#-core-logic)
- [Real Hardware Setup](#-real-hardware-setup)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [Testing](#-testing)
- [User Roles](#-user-roles)
- [Database Model](#-database-model)
- [Project Status](#-project-status)
- [AI Assistance](#-ai-assistance)
- [License](#-license)
- [Author](#-author)

---

## ⚙️ Core Logic

```
1. Battery charged (≥ 90%, configurable) AND PV surplus > 300 W (configurable)?
2. Surplus stable over 2 measurement cycles (120s) → flapping protection
3. → Raise heat pump setpoint from last_known_setpoint (default 47°C) to 55°C
4. Surplus ends → automatically reset to last_known_setpoint
5. Safety limits: Min 45°C / Max 60°C (configurable)
6. EnerFlow state persisted in DB (enerflow_state table) → restart safety
7. Toggle (ROLE_USER): activate / deactivate at any time, independent of automation
```

### Physical Basis

```
Q [kWh] = Volume [L] × 0.001163 × ΔT [K]
```

Example: 280-litre hot water tank, ΔT = 8 K (47°C → 55°C), COP ≈ 4.0 (Helox 5)
→ roughly 0.7–0.8 kWh of electrical energy saved per boost cycle.

---

## 🏠 Real Hardware Setup

| Device | Model | Interface |
|---|---|---|
| Heat Pump | Novelan Helox 5, HSV 280 tank | WebSocket port 8214 (monitoring, `Lux_WS` protocol) + myUplink Cloud REST API (control) |
| Battery | sonnenBatterie, 11 kWh | local REST API |
| PV System | Panasonic HIT module, 5.7 kWp | data via Sonnen battery |
| Inverter | SolarEdge SE7K | not directly integrated |

> **Note on heat pump control:** Firmware V3.92.2 of the Helox 5 does not accept hot-water
> setpoint commands over WebSocket (confirmed against the real unit). Setpoint control is
> therefore handled exclusively through the **myUplink Cloud REST API**, while the WebSocket
> connection remains in use for real-time monitoring only.

---

## 🏗️ Architecture

EnerFlow follows the **Hexagonal Architecture (Ports & Adapters)** pattern — the core never
depends on a concrete vendor implementation, only on interfaces.

```
de.saki.enerflow
├── core/
│   ├── domain/          ← JPA entities (EnerflowState, BatterySnapshot, HeatpumpSnapshot,
│   │                       ControlLog, DeviceConfig, ElectricityPriceConfig, AppUser, …)
│   ├── repository/      ← Spring Data repositories
│   └── service/         ← EnergyManagerService (core logic), DeviceConfigService,
│                           HotWaterEnergyCalculator, SnapshotRetentionService,
│                           EnerFlowUserDetailsService, LoginAttemptService
├── adapter/
│   ├── heatpump/
│   │   ├── novelan/     ← NovelanHeatpumpClient (WebSocket, monitoring only)
│   │   └── myuplink/    ← MyUplinkRestAdapter, MyUplinkTokenService (setpoint control)
│   └── battery/
│       └── sonnen/      ← SonnenBatteryAdapter, SonnenBatterySnapshotService
├── api/
│   ├── dashboard/       ← DashboardController, DashboardService
│   └── config/          ← ElectricityPriceController
├── adapter/web/dto/     ← EnerflowStateController + DTOs
└── config/
    └── security/        ← SecurityConfig, JwtService, JwtAuthenticationFilter
```

**Key principle:** New hardware vendor = new adapter behind an existing interface, no changes
to the core automation logic.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Temurin) |
| Framework | Spring Boot 3.5.15 |
| Build | Maven |
| Database | PostgreSQL 16 (Docker), H2 for the test profile |
| ORM / Migration | Spring Data JPA, Hibernate 6.6, Flyway |
| Security | Spring Security 6.5, BCrypt, JWT (jjwt 0.13.0) |
| Heat Pump Client (monitoring) | Java-WebSocket 1.6.0 |
| Heat Pump Client (control) | Spring REST client against myUplink Cloud API |
| Battery Client | REST against the local Sonnen API |
| Frontend | Thymeleaf, Bootstrap 5.3.8, vanilla JS |
| Container | Docker (multi-stage build), Docker Compose |
| Testing | JUnit 5, Mockito, AssertJ, Spring Test (`@WebMvcTest`) |
| Secrets | dotenv-java (`.env` file, never committed) |

---

## 🚀 Getting Started

### Prerequisites

- Docker Desktop (includes Docker Compose)
- For local (non-container) development: Java 21 (Temurin), Maven, IntelliJ IDEA recommended
- Network access to a Novelan heat pump and Sonnen battery for full functionality
  (the app will still start and run its test suite without them)

### Option A — Run everything in Docker (recommended)

```bash
git clone https://github.com/AthanasiosDiamantis/enerflow.git
cd enerflow
cp .env.example .env   # fill in your local values, see below
docker compose up --build
```

This builds and starts two containers: `enerflow-app` (the Spring Boot application) and
`enerflow-postgres` (PostgreSQL 16). The app waits for the database healthcheck before starting.
Once it's up, open **http://localhost:8080/dashboard**.

### Option B — Run the app locally, database in Docker

```bash
cd enerflow
docker compose up -d postgres
cd enerflow-app
cp .env.example .env   # fill in your local values
./mvnw spring-boot:run
```

### Required `.env` values

```env
POSTGRES_PASSWORD=your_db_password
HEATPUMP_PASSWORD=your_myuplink_client_secret
SONNEN_HOST=192.168.xxx.xxx
SONNEN_API_TOKEN=your_sonnen_token
ENERFLOW_DEFAULT_ADMIN_PASSWORD=your_initial_admin_password
ENERFLOW_JWT_SECRET=your_jwt_secret_min_32_bytes
ENERFLOW_JWT_EXPIRATION_MS=86400000
```

> ⚠️ **Never commit `.env` to Git** — it's already listed in `.gitignore`.

> **Note:** When running via Docker Compose, `POSTGRES_PASSWORD` is only applied the *first*
> time the database volume is created. If you change it afterwards, reset the volume with
> `docker compose down -v` before starting again.

### Stopping the app

```bash
docker compose stop      # stop containers, keep data
docker compose down      # stop and remove containers (data volume is preserved separately)
```

---

## 🧪 Testing

The project ships with **80 automated tests** across unit, web-slice, and integration levels.

```bash
mvn test                       # 72 tests — unit and web-slice tests only
mvn test -DexcludedGroups=      # 80 tests — includes integration tests against real hardware/APIs
```

Integration tests (`@Tag("integration")`, class suffix `IT`) call the real myUplink and Sonnen
APIs directly and are excluded from the default run (`pom.xml` → `excludedGroups=integration`),
since they require network access to real devices. A full breakdown of all test classes and
their mapping to functional test scenarios is documented in
[`docs/EnerFlow_Testprotokoll_Sprint5.docx`](docs/EnerFlow_Testprotokoll_Sprint5.docx).

---

## 👥 User Roles

| Role | Technical | Permissions |
|---|---|---|
| Homeowner | `ROLE_USER` | Read dashboard, activate/deactivate EnerFlow, configure electricity price |
| Plant Manager | `ROLE_MANAGER` | + device configuration, thresholds, tank sizes, manual control |
| Administrator | `ROLE_ADMIN` | + user management, system configuration |

Accounts lock for 15 minutes after 5 failed login attempts. Authentication uses JWT bearer
tokens (8-hour validity by default).

---

## 🗄️ Database Model

| Table | Content | Frequency |
|---|---|---|
| `device_config` | Tank size, thresholds, polling interval (key–value) | On change |
| `enerflow_state` | `enabled`, `boost_active`, `last_known_setpoint` (singleton) | On change |
| `heatpump_snapshot` | Temperatures, operating state | Every 60s |
| `battery_snapshot` | SOC, PV production, consumption, grid feed-in | Every 60s |
| `control_log` | Control actions (RAISE/RESET) + kWh calculation — append-only audit log | On action |
| `app_user` | Login, BCrypt hash, role, lockout state | On change |
| `electricity_price_config` | Current electricity price ct/kWh (singleton) | On change |
| `electricity_price_change_log` | Audit log for price changes | On change |

---

## 📅 Project Status

All five planned sprints are complete. The application runs in production against real
hardware, is containerized, and is backed by an automated test suite.

| Sprint | Focus | Status |
|---|---|---|
| Sprint 1 | Device interfaces (heat pump + battery) | ✅ Done |
| Sprint 2 | Automation logic + safety limits | ✅ Done |
| Sprint 3 | Data persistence + security (JWT, roles) | ✅ Done |
| Sprint 4 | Dashboard & UI | ✅ Done |
| Sprint 5 | Docker, automated tests, test protocol | ✅ Done |

Detailed sprint dates and work packages: see `EnerFlow_PSP_v1_4.xlsx` under `docs/`.

**Not implemented (deliberately deferred):** grid feed-in restriction window with a lowered
threshold, graphical configuration UI for device/system parameters (currently DB-driven only),
multi-tenant support.

---

## 🤖 AI Assistance

This project was developed with active use of **Claude Sonnet 4.6 and 5** (Anthropic) for
project documentation, architecture decisions, code reviews, and implementation support.
All technical decisions, system design, and final responsibility lie with the developer.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Athanasios Diamantis**

- 📧 Email: [athanasios.diamantis@web.de](mailto:athanasios.diamantis@web.de)
- 💼 LinkedIn: [linkedin.com/in/athanasios-diamantis](https://www.linkedin.com/in/athanasios-diamantis/)
- 🔗 Xing: [xing.com/profile/Athanasios_Diamantis2](https://www.xing.com/profile/Athanasios_Diamantis2)
- 🐙 GitHub: [@AthanasiosDiamantis](https://github.com/AthanasiosDiamantis)
- 📁 Project: Portfolio project for Java developer job applications (2026)