# Shared Testing Rules

Primary reference:

- `ai-dev/TEST-SPEC.md`

Use this shared reference when a skill package needs project-level testing expectations.

## Repo-Level Expectations

- Tests are part of the task, not optional follow-up work.
- Anti-goals should be represented in tests when applicable.
- Illegal state transitions should be tested with explicit failure assertions.
- Generated tests should use business-facing semantics in names and descriptions where the stack
  supports it.

## Skills That Depend On This

- `generate-api`
- `generate-test`
- `code-review`
