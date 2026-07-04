# Gym CRM (Spring Core)

A simple in-memory Gym CRM system built with **Spring Core** (no Spring Boot).
Manages trainees, trainers and trainings with profile creation, username/password
generation and CSV-based data initialization at startup.

# TODO: 
replace long parameter lists in service methods (create/update)
with request DTOs — planned for the REST module.

## Tech Stack

- Java 17
- Spring Core 6 (annotation-based configuration)
- Lombok
- OpenCSV
- SLF4J + Logback
- JUnit 5, Mockito, AssertJ
- Maven


### Key components

| Layer | Responsibility |
|-------|----------------|
| **Facade** | Single entry point aggregating all services |
| **Service** | Business logic (CRUD, username/password generation) |
| **DAO** | In-memory persistence using `Map<Long, Entity>` |
| **Util** | `UsernameGenerator`, `PasswordGenerator` |
| **Init** | `StorageInitBeanPostProcessor` loads CSV data into storage |

## Domain Model

- **User** (base) → `Trainee`, `Trainer`
- **Trainee**: dateOfBirth, address
- **Trainer**: specialization (`TrainingType`)
- **Training**: links a trainee and trainer
- **TrainingType**: enum-backed (`FITNESS`, `YOGA`, `STRENGTH`, ...)

## Features

- Create / update / delete / select **Trainee**
- Create / update / select **Trainer**
- Create / select **Training**
- Automatic **username** generation (`FirstName.LastName`, with serial suffix on collision)
- Automatic random **password** generation (10 chars)
- CSV-based data loading on startup
- Passwords are masked in logs

## Configuration

File paths are defined in `src/main/resources/application.properties`:

```properties
storage.trainee.file=data/trainees.csv
storage.trainer.file=data/trainers.csv
storage.training.file=data/trainings.csv
```

## Build & Run
Build the project:

```bash
mvn clean install
```
Run the demo (App.java):

```bash
mvn exec:java -Dexec.mainClass="com.epam.gym.App"
```

### Tests
Run all unit tests:

```bash
mvn test
```
### Tests coverage

<img width="546" height="247" alt="Знімок екрана 2026-06-23 154403" src="https://github.com/user-attachments/assets/5d332a8f-deff-4237-b106-bcdd993747ae" />

