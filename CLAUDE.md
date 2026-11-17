# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`ecos-process` is the process management service for the Citeck ECOS platform. It handles BPMN (Business Process Model and Notation) and CMMN (Case Management Model and Notation) workflow execution, DMN (Decision Model and Notation) decisions, and process task management using Camunda BPM engine.

**Technology Stack:**
- Language: Kotlin (primary) and Java
- Framework: Spring Boot with Camunda BPM 7.21.0
- Database: MongoDB for process data, PostgreSQL for Camunda engine data
- Build Tool: Maven
- Key Libraries: ecos-webapp-lib, camunda-bpm-spring-boot-starter, QueryDSL, Lombok

## Development Commands

### Running the Application

For agent-managed local Maven/Spring Boot launches, prefer `spring-boot:start`, **not `spring-boot:run`**. Wait for readiness and let Maven exit: keeping Maven alive retains compilation memory for the lifetime of the service. This does not change services already managed by Citeck Launcher/containers; inspect the existing instance first and do not launch duplicates.

```bash
# JVM_ARGS must come from the confirmed ecos-process local environment.
./mvnw spring-boot:start -Dkotlin.compiler.daemon=false \
  -Dspring-boot.start.maxAttempts=240 \
  -Dspring-boot.run.jvmArguments="$JVM_ARGS"
# Stop with the same Maven plugin / JMX configuration used for start.
./mvnw spring-boot:stop
```

Agent requirements:
- Use the project Maven wrapper. Disable the Kotlin compiler daemon with `-Dkotlin.compiler.daemon=false` and verify no new compiler daemon remains; this flag does not stop pre-existing daemons.
- `maxAttempts=240` with the default 500 ms wait allows up to 120 seconds for startup. JVM arguments for `start` still use `spring-boot.run.jvmArguments`; `MAVEN_OPTS` is not a substitute for application JVM arguments.
- Heap limits, active profiles, network overrides and credentials must come from this service's instructions and confirmed local environment. Do not copy citeck-uni's 256 MiB heap limit or increase an existing limit without approval. Do not put private addresses or credentials in shared instructions.
- Confirmed local Launcher `eproc` memory settings (checked 2026-11-16): container limit **2 GiB**, JVM **`-Xmx1g`**, **no explicit `-Xms`** (leave the JVM default; do not invent `-Xms1g`). Other memory caps are `-XX:MaxDirectMemorySize=266m -XX:MaxMetaspaceSize=384m -XX:ReservedCodeCacheSize=122m`. For a local Maven start against this environment, preserve these JVM caps in `JVM_ARGS`; the 2 GiB container limit is not an `-Xmx` value. These are per-developer reference settings, not universal production defaults: re-read Launcher settings when the target changes. Do not copy container-only paths (e.g. heap-dump destinations), profiles or network settings blindly into a host JVM.
- Concurrent services need distinct application and JMX ports. The default JMX port is 9001; configure `jmxPort` in the Maven plugin identically for start and stop, rather than assuming an undocumented CLI property.
- Write logs outside git and ensure the application JVM survives the launching terminal/supervisor exiting. Record the app PID, log path and JMX configuration: a completed start command no longer supervises the application.
- After startup, verify application health and gateway availability, the actual heap flags (`jcmd <app-pid> VM.flags`), that Maven has exited, and that no new Kotlin compiler daemon remains. Maven success alone is not a readiness check.
- Stop only verified, authorized processes via `spring-boot:stop` with the matching configuration, or the verified app PID when necessary. Never use broad `pkill`, stop other sessions' JVMs or stop platform containers to free memory. If Launcher manages the instance, use its available tools for logs and authorized restarts.

You can also run the main class `ru.citeck.ecos.process.EprocApp` directly from your IDE.

### Testing

Run all tests:
```bash
./mvnw clean test
```

Run a specific test class:
```bash
./mvnw test -Dtest=YourTestClass
```

Run a specific test method:
```bash
./mvnw test -Dtest=YourTestClass#testMethod
```

### Code Quality

Run ktlint to check Kotlin code style:
```bash
./mvnw ktlint:check
```

Format Kotlin code with ktlint:
```bash
./mvnw ktlint:format
```

Start Sonar server for code analysis:
```bash
docker compose -f docker/sonar.yml up -d
```

Run Sonar analysis:
```bash
./mvnw -Pprod clean test sonar:sonar
```

### Building for Production

Build Docker image with custom tag:
```bash
./mvnw -Pprod clean package jib:dockerBuild -Djib.docker.image.tag=custom
```

## Architecture

### Domain-Driven Design Structure

The codebase follows a domain-driven design with each domain module in `src/main/java/ru/citeck/ecos/process/domain/`:

- **bpmn/** - BPMN process engine integration, Camunda services, process definitions, and execution
- **cmmn/** - CMMN case management engine and models
- **dmn/** - DMN decision management
- **proctask/** - Process task management, task attributes synchronization, task queries
- **procdef/** - Process definition management (BPMN, CMMN, DMN) and versioning
- **proc/** - Generic process lifecycle and state management
- **bpmnsection/** - BPMN section organization and permissions
- **dmnsection/** - DMN section organization and permissions
- **bpmnreport/** - BPMN process reporting and analytics
- **bpmnla/** - BPMN lazy approval integration
- **timer/** - Timer and scheduled task management
- **actions/** - Process-related actions and commands

### Key Architectural Patterns

**Records API Pattern:**
- The application exposes data through Records API (RecordsDao pattern)
- Each domain has Records classes (e.g., `BpmnProcessRecords`, `ProcTaskRecords`) that implement:
  - `RecordAttsDao` - for reading attributes
  - `RecordMutateDao` - for mutations
  - `RecordsQueryDao` - for querying
- Records are referenced via EntityRef: `eproc/bpmn-proc@{id}`

**Service Layer:**
- Each domain has a service interface and implementation
- Services coordinate between Camunda engine and ECOS records/data
- Example: `BpmnProcessService` orchestrates process instances with Camunda's `RuntimeService`

**Engine Integration:**
- Camunda BPM engine is tightly integrated at `domain/bpmn/engine/camunda/`
- Custom Camunda services are in `domain/bpmn/engine/camunda/services/beans/`
- Event handling bridges Camunda events to ECOS event system via `BpmnEventEmitter`

**Process Definition Management:**
- All process types (BPMN, CMMN, DMN) are versioned and stored via `ProcDefService`
- Process definitions support workspaces for multi-tenancy
- Auto-starting processes are handled by `BpmnProcessAutoStarter`

### MongoDB & QueryDSL

- MongoDB is used for process definitions, tasks, and ECOS-specific data
- QueryDSL is used for type-safe queries on MongoDB entities
- QueryDSL Q-classes are generated via Kotlin annotation processing (kapt)
- MongoDB repositories are auto-discovered with `@EnableMongoRepositories("ru.citeck.ecos.process.domain.*.repo")`

### Testing Patterns

- Integration tests use `@SpringBootTest(classes = EprocApp.class)`
- Camunda testing utilities: `camunda-bpm-junit5`, `camunda-bpm-assert-scenario`
- Testcontainers for MongoDB in tests
- Test extension: `@ExtendWith(EcosSpringExtension.class)`

## Important Concepts

### Process Types
- `BPMN_PROC_TYPE` = "bpmn" - Business processes
- `CMMN_PROC_TYPE` = "cmmn" - Case management
- Each has its own Records API endpoint

### Workspace Support
All process definitions support workspaces via `WorkspaceService` for data isolation and multi-tenancy.

### Process Variables
- System variables prefixed with `SYS_VAR_PREFIX` (usually "_")
- Document reference: `BPMN_DOCUMENT`, `BPMN_DOCUMENT_REF`, `BPMN_DOCUMENT_TYPE`
- Workflow initiator: `BPMN_WORKFLOW_INITIATOR`

### Task Attributes Synchronization
The `proctask/attssync` module synchronizes task attributes with external records for real-time updates.

### EApps Artifacts
The application uses ECOS artifacts system:
- Artifacts are in `src/main/resources/eapps/artifacts/`
- Includes UI definitions (journals, forms, dashboards), i18n, actions
- Artifact handlers in each domain's `eapps/` package

## Dependencies

The application requires these ECOS services:
- **zookeeper** - Configuration management
- **rabbitmq** - Message broker for events
- **ecos-model** - Type and model management
- **ecos-registry** - Service registry

## File Locations

- Main application: `src/main/java/ru/citeck/ecos/process/EprocApp.java`
- Configuration: `src/main/resources/config/application.yml` (dev: application-dev.yml)
- Liquibase migrations: `src/main/resources/ecos/liquibase/camunda/`
- Test configuration: `src/test/resources/config/application-test.yml`

## Common Development Patterns

### When Adding a New Process Element
1. Create model classes in `domain/{process-type}/model/`
2. Add converters for I/O in `domain/{process-type}/io/convert/`
3. Update the corresponding service (e.g., `BpmnProcessService`)
4. Add Records API support if needed
5. Write integration tests

### When Working with Camunda Services
- Access engine services via dependency injection: `RuntimeService`, `RepositoryService`, `HistoryService`, `TaskService`
- Custom script beans are in `domain/bpmn/engine/camunda/services/beans/`
- Use `CamundaEntityId` for entity reference conversions

### Logging
- Uses kotlin-logging (KotlinLogging.logger {})
- Configured in `src/main/resources/logback-spring.xml`
- Be mindful of large output logs (recent fix: d3e73d3e)

## Main Branch

- Default branch: `develop`
- Main branch for PRs: `master`
