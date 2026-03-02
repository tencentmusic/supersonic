# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Backend (Java/Maven)

```bash
# Clean build (skip tests)
mvn clean package -DskipTests -Dspotless.skip=true

# Run all tests
mvn test

# Run single test class
mvn test -Dtest=ClassName

# Full CI build
mvn -B package --file pom.xml
```

**Requirements:** Java 21, Maven

### Frontend (pnpm/React)

```bash
cd webapp

# Install dependencies
pnpm install

# Start dev server (port 9000)
pnpm dev

# Production build
pnpm build

# Run tests
pnpm test
```

**Requirements:** Node.js >=16, pnpm 9.12.3+

### Quick Start

```bash
# Build full release
./assembly/bin/supersonic-build.sh standalone

# Start service
./assembly/bin/supersonic-daemon.sh start

# Stop service
./assembly/bin/supersonic-daemon.sh stop
```

Visit http://localhost:9080 after startup.

## Architecture Overview

SuperSonic unifies **Chat BI** (LLM-powered) and **Headless BI** (semantic layer) paradigms.

### Core Modules

```
supersonic/
├── auth/           # Authentication & authorization (SPI-based)
├── chat/           # Chat BI module - LLM-powered Q&A interface
├── common/         # Shared utilities
├── headless/       # Headless BI - semantic layer with open API
├── launchers/      # Application entry points
│   ├── standalone/ # Combined Chat + Headless (default)
│   ├── chat/       # Chat-only service
│   └── headless/   # Headless-only service
└── webapp/         # Frontend React app (UmiJS 4 + Ant Design)
```

### Data Flow

1. **Knowledge Base**: Extracts schema from semantic models, builds dictionary/index for schema mapping
2. **Schema Mapper**: Identifies metrics/dimensions/entities/values in user queries
3. **Semantic Parser**: Generates S2SQL (semantic SQL) using rule-based and LLM-based parsers
4. **Semantic Corrector**: Validates and corrects semantic queries
5. **Semantic Translator**: Converts S2SQL to executable SQL

### Key Entry Points

- `StandaloneLauncher.java` - Combined service with `scanBasePackages: ["com.tencent.supersonic", "dev.langchain4j"]`
- `ChatLauncher.java` - Chat BI only
- `HeadlessLauncher.java` - Headless BI only

## Key Technologies

**Backend:** Spring Boot 3.3.9, MyBatis-Plus 3.5.10.1, LangChain4j 0.36.2, JSqlParser 4.9, Calcite 1.38.0

**Frontend:** React 18, UmiJS 4, Ant Design 5.17.4, ECharts 5.0.2, AntV G6/X6

**Databases:** MySQL, PostgreSQL (with pgvector), H2, ClickHouse, StarRocks, Presto, Trino, DuckDB

## Testing

**Java tests:** JUnit 5, Mockito. Located in `src/test/java/` of each module.

**Frontend tests:** Jest with Puppeteer environment in `webapp/packages/supersonic-fe/`

**Evaluation scripts:** Python scripts in `evaluation/` directory for Text2SQL accuracy testing.

## Related Documentation

- [README.md](README.md) - English documentation
- [README_CN.md](README_CN.md) - Chinese documentation
- [Evaluation Guide](evaluation/README.md) - Text2SQL evaluation process
