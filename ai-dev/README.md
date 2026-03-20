# AI Engineering References

`ai-dev/` is the engineering reference workspace for this repo.

## Structure

- `shared/`: shared rules and cross-cutting references
- `evals/`: structured regression cases for prompt quality
- `ai-spec/`: domain contracts such as intent, entities, state machines, OpenAPI, and rule engine
- `PROMPT-VERSIONS/`: versioned prompt templates with changelogs
- top-level guides such as `SPEC-DISCOVERY.md`, `CONTEXT-SLICE.md`, `TEST-SPEC.md`,
  `VIBE-CHECKLIST.md`, and `AGENT-PROTOCOL.md`

## How to Use

**Process guidance**
- Read `SPEC-DISCOVERY.md` to clarify scope, anti-goals, and compliance constraints
- Read `CONTEXT-SLICE.md` to pick the minimal file set for the current task
- Read `TEST-SPEC.md` when adding or changing tests
- Read `VIBE-CHECKLIST.md` and `AGENT-PROTOCOL.md` for review expectations

**Prompt references**
- `PROMPT-VERSIONS/generate-api/` for backend CRUD and API generation guidance
- `PROMPT-VERSIONS/generate-entity/` for DO/Mapper/Service generation guidance
- `PROMPT-VERSIONS/generate-test/` for test generation guidance
- `PROMPT-VERSIONS/code-review/` for code review guidance
- `PROMPT-VERSIONS/generate-migration/v1.0.md` for Flyway MySQL and PostgreSQL migrations
- `PROMPT-VERSIONS/generate-frontend/` for React and TypeScript page generation guidance

## Principles

- `ai-dev/` is a reference directory, not a runtime skill registry.
- Anti-Goal coverage is mandatory in every test suite; see `ai-spec/domain/intent.md`.
- MySQL migrations must never use `ADD COLUMN IF NOT EXISTS`; see `CLAUDE.md`.
- Prompt versions should be regression-checked against `evals/` cases.
