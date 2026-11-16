# 🛡 Digital Crime Evidence Management System (DCEMS)

CSE 309 – Java Lab Project (**Outstanding** category)
**JavaFX** (GUI + Event Handling) · **MySQL** (JDBC, rich operations) · Maven

A desktop app for capturing digital evidence, protecting it with SHA-256 fingerprints, and tracking the full
chain of custody for every item – from the crime scene to the courtroom.

---

## 1. How it maps to the evaluation rubric

| Rubric (Outstanding) | Where in this project |
|---|---|
| **Event Handling** | Button/mouse/keyboard events, search-as-you-type listeners, drag-&-drop file events, double-click rows, `Enter` to log in, selection listeners, background `Task` completion events |
| **JavaFX** | Every screen: `BorderPane`, `HBox/VBox`, `TableView`, `PieChart`, `BarChart`, `Dialog`, `FileChooser`, animations (`Timeline`, `FadeTransition`), external CSS |
| **Oracle/MySQL with rich operations** | MySQL via JDBC `PreparedStatement`s – full CRUD, JOINs, `GROUP BY` aggregates, sub-queries, dynamic multi-filter search, foreign keys with `CASCADE`/`SET NULL`, auto-created schema + seed data |

### Rich database operations (examples)
* **CRUD** on cases, evidence, suspects, users, custody entries
* **JOIN** queries (evidence ⇄ case ⇄ user), **sub-query** for evidence count per case
* **Aggregates** (`COUNT`, `GROUP BY`) for dashboard charts and the case-summary report
* **Dynamic search/filter** (text + type + status) on the evidence vault
* **Referential integrity** – deleting a case removes its evidence and custody history
* **Audit trail** – `activity_log` and `custody_log` tables

---

## 2. Features

| Screen | What you can do |
|---|---|
| **Login** | Secure sign-in (SHA-256 hashed passwords), role-based menu (Admin sees *Users*) |
| **Dashboard** | Animated stat cards, quick actions, evidence-by-type pie chart, cases-by-status bar chart, recent evidence, live activity feed |
| **Cases** | Create / edit / delete / search cases, priority, status, assignee |
| **Capture Evidence** | Drag & drop (or browse) a file → SHA-256 fingerprint calculated in the background → image preview → protected copy saved in `evidence_store/` → first custody entry written automatically |
| **Evidence Vault** | Search + filter, details dialog, **Verify Integrity** (detects tampering), change status, transfer custody, delete |
| **Suspects** | Register suspects and link them to cases |
| **Chain of Custody** | Complete audit timeline, filter by evidence, add new custody events |
| **Reports** | Export Evidence / Cases / Suspects / Custody CSV, generate and save a per-case text report, case summary table |
| **Users** (admin) | Create and delete investigator accounts |

### Demo logins
| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Admin |
| `investigator` | `invest123` | Investigator |
| `analyst` | `analyst123` | Analyst |

---

## 3. Folder structure

```
DCEMS/
├── pom.xml                         Maven build (JavaFX 17 + MySQL connector)
├── README.md
├── run.bat / run.sh                One-click launchers
├── database/
│   ├── schema.sql                  Table definitions (also auto-created by the app)
│   └── sample_queries.sql          JOIN / GROUP BY queries for your viva
├── evidence_store/                 Protected copies of captured evidence files
└── src/main/
    ├── java/com/dcems/
    │   ├── App.java                Application entry point
    │   ├── db/
    │   │   ├── Database.java       JDBC connection + query helpers, auto-setup
    │   │   ├── Schema.java         CREATE TABLE statements
    │   │   └── SeedData.java       Default users + demo data
    │   ├── model/                  User, CaseFile, Evidence, Suspect, CustodyEntry, Activity
    │   ├── dao/                    UserDAO, CaseDAO, EvidenceDAO, SuspectDAO,
    │   │                           CustodyDAO, ActivityDAO, StatsDAO
    │   ├── ui/                     LoginView, MainView (shell), DashboardView, CasesView,
    │   │                           EvidenceCaptureView, EvidenceVaultView, SuspectsView,
    │   │                           CustodyView, ReportsView, UsersView
    │   └── util/                   UI (reusable widgets), Toast, Hash, Csv, Fmt, Session
    └── resources/
        ├── db.properties           ⚙ MySQL host / user / password
        └── css/style.css           All styling
```

---

## 4. How to run

**Requirements:** JDK 17+, Maven 3.8+, MySQL 8 (or MariaDB) running locally.

1. Start MySQL.
2. Open `src/main/resources/db.properties` and set your MySQL **user** and **password**
   (default is `root` / `root`).
3. Run:
   ```
   mvn javafx:run          (or double-click run.bat on Windows / ./run.sh on Linux & macOS)
   ```
   The database `dcems_db`, all tables and demo data are created automatically on first launch.

**IDE (IntelliJ / Eclipse / NetBeans):** open the folder as a *Maven project*, let it download dependencies,
then run `com.dcems.App` (if your IDE complains about missing JavaFX runtime components, use the Maven goal `javafx:run`).

### Troubleshooting
* *"Cannot connect to MySQL"* → MySQL not running or wrong password in `db.properties`.
* *Access denied for user* → create a user / fix the password; the app needs permission to `CREATE DATABASE`.
* Emoji icons look plain on some Linux systems – install `fonts-noto-color-emoji`.

---

## 5. Viva talking points
1. **Why SHA-256?** A cryptographic fingerprint taken at capture time; if even one byte of the stored file changes, *Verify Integrity* fails → proves tampering.
2. **Chain of custody:** every handover is an immutable row in `custody_log` (who, to whom, when, why).
3. **Layered design:** `ui` (JavaFX) → `dao` (SQL) → `db` (JDBC). No SQL inside UI classes.
4. **Safety:** all queries use `PreparedStatement` (prevents SQL injection); passwords are stored hashed, never in plain text.
5. **Concurrency:** large-file hashing runs in a JavaFX `Task` so the UI never freezes.
