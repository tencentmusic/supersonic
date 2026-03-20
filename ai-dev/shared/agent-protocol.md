# Shared Agent Protocol

Source of truth:

- `ai-dev/AGENT-PROTOCOL.md`

This file exists to give the skill-pack system a stable shared reference.

## Shared Rules

- Every implementation handoff should include the spec reference, changed code, tests, and the
  skill or prompt version used.
- Review should validate architecture conformance, test coverage, and anti-goal coverage before
  approving quality.
- If a skill result changes repo behavior, the downstream skill should be able to identify:
  - what changed
  - why it changed
  - how to validate it

## Required Handoff Elements

- spec or intent reference
- code or planned changes
- test artifacts or expected test coverage
- review notes or checklist outcome
- skill version used

## Shared Blocking Conditions

- missing tests for behavior-changing work
- missing anti-goal coverage for guarded paths
- unclear contract between produced output and downstream consumer
