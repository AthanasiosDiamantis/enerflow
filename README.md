# ⚡ EnerFlow – Intelligent Energy Management System

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)
![Maven](https://img.shields.io/badge/Maven-3.x-red?logo=apachemaven)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Status](https://img.shields.io/badge/Status-In%20Development-blue)

> **Portfolio Project** — developed as part of a Java qualification program (May–July 2026)

EnerFlow is a local energy management system that intelligently connects a photovoltaic (PV) system,
a battery storage unit, and a heat pump. Excess solar energy is automatically converted into thermal
energy by dynamically raising the heat pump's hot water target temperature — instead of feeding
unused electricity back into the grid.

---

## 🏠 Real Hardware Setup

| Device | Model | Interface |
|---|---|---|
| Heat Pump | Novelan Helox 5, WPR 2.0 | WebSocket Port 8214 (Lux_WS protocol) |
| Battery | sonnenBatterie 10, 11 kWh | REST API `/api/v2/status` |
| PV System | Panasonic HIT Module, 5.7 kWp | Data via Sonnen battery |
| Inverter | SolarEdge SE7K | Not directly integrated (v1.1) |

---

## ⚙️ Core Logic

```
1. Battery charged (≥ 90%) AND PV surplus > 300 W?
2. Surplus stable over 2 measurement cycles (120s) → Flapping protection
3. → Raise heat pump setpoint from last_known_setpoint (default 47°C) to 55°C
4. Surplus ends → automatically reset to last_known_setpoint
5. Grid feed-in restriction window (11–15h): lower threshold to 70%
6. Safety limits: Min 45°C / Max 60°C (all values configurable)
7. EnerFlow state persisted in DB → restart safety
8. Toggle (ROLE_USER): activate / deactivate at any time
```

### Physical Basis

```
Q [kWh] = (m [kg] × c [kJ/kg·K] × ΔT [K]) / 3600
```

Example: 300-litre hot water tank, ΔT = 8 K (47°C → 55°C) → Q ≈ 2.8 kWh

---

## 🏗️ Architecture

EnerFlow follows the **Hexagonal Architecture (Ports & Adapters)** pattern:

```
de.saki.enerflow
├── core/
│   ├── model/          ← Interfaces: EnergySource, HeatGenerator, EnergyStorage
│   ├── service/        ← EnergyManagerService (core logic)
│   └── repository/     ← DB interfaces
├── adapter/
│   ├── heatpump/
│   │   └── novelan/    ← HeatpumpClient (WebSocket)
│   ├── battery/
│   │   └── sonnen/     ← BatteryClient (REST)
│   └── pv/
│       └── generic/
├── api/                ← REST controllers (Spring Boot)
├── dashboard/          ← Frontend
└── scheduler/          ← @Scheduled polling (60s) + control logic
```

**Key principle:** Never program against concrete classes — always against interfaces.
New hardware vendor = new adapter, no changes to the core.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Temurin) |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Database | PostgreSQL 16 (Docker) |
| ORM / Migration | Spring Data JPA, Hibernate, Flyway |
| Security | Spring Security 6, BCrypt, JWT |
| Heat Pump Client | Java-WebSocket 1.6.0 |
| Battery Client | Spring WebClient (WebFlux) |
| JSON | Jackson 3.x |
| Container | Docker, Docker Compose |
| Secrets | dotenv-java (.env file, never in Git) |

---

## 👥 User Roles

| Role | Technical | Permissions |
|---|---|---|
| Homeowner | `ROLE_USER` | Read dashboard, view history, activate/deactivate EnerFlow, configure electricity price |
| Plant Manager | `ROLE_MANAGER` | + Device configuration, thresholds, tank sizes, time windows, manual control |
| Administrator | `ROLE_ADMIN` | + User management, system configuration |

---

## 🗄️ Database Model

| Table | Content | Frequency |
|---|---|---|
| `device_config` | Tank sizes, IP, thresholds, polling & dashboard interval | On change |
| `enerflow_state` | `enerflow_active`, `last_known_setpoint`, `last_set_by_enerflow` | On change |
| `heatpump_snapshot` | Temperatures, power, operating state | Every 60s |
| `battery_snapshot` | SOC, PV, consumption, grid | Every 60s |
| `control_log` | Control actions (RAISE/RESET/MANUAL) + kWh calculation — append only | On action |
| `device_info` | Serial number, software version, IP | On change |
| `app_user` | Login, BCrypt hash, role | On change |

---

## 🚀 Getting Started

### Prerequisites

- Java 21 (Temurin)
- Maven 3.x
- Docker & Docker Compose
- Access to a local network with Novelan heat pump and Sonnen battery

### 1. Clone the repository

```bash
git clone https://github.com/AthanasiosDiamantis/enerflow.git
cd enerflow/enerflow-app
```

### 2. Create `.env` file

```bash
cp .env.example .env
```

Edit `.env` with your local values:

```env
HEATPUMP_HOST=192.168.xxx.xxx
HEATPUMP_PASSWORD=your_password
SONNEN_HOST=192.168.xxx.xxx
SONNEN_API_TOKEN=your_token
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret
```

> ⚠️ **Never commit `.env` to Git!** It is listed in `.gitignore`.

### 3. Start PostgreSQL via Docker

```bash
docker compose up -d
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

---

## 📅 Project Roadmap

| Version | Scope | Goal |
|---|---|---|
| **v1.1** (current) | Device interfaces, automation, security, dashboard, Docker | Portfolio / Job application |
| v2.0 | Multi-tenant, additional vendor adapters, weather forecast, heating circuit control | Production-ready |
| v3.0 | Cloud option, 100,000+ plant dashboard, mobile app | SaaS product |

### Sprint Plan (v1.1)

| Sprint | Period | Focus |
|---|---|---|
| Sprint 1 | 28.05–03.06.2026 | Device interfaces (heat pump + battery) |
| Sprint 2 | 04.06–10.06.2026 | Automation logic + safety |
| Sprint 3 | 11.06–17.06.2026 | Data persistence + security |
| Sprint 4 | 18.06–24.06.2026 | Dashboard & UI |
| Sprint 5 | 25.06–01.07.2026 | Docker, tests, documentation |

---

## 🤖 AI Assistance

This project was developed with active use of **Claude Sonnet 4.6** (Anthropic) for project
documentation, architecture decisions, code reviews, and implementation support.
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