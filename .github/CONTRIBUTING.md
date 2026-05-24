# Contributing to Eventus

Thank you for your interest in contributing!

## Running the tests

```bash
mvn verify
```

This builds all modules and runs unit and integration tests.

## Pull request guidelines

- One feature or fix per PR — keep the scope small and reviewable.
- Write a clear PR description explaining *why* the change is needed.
- All tests must pass (`mvn verify`) before requesting review.
- Follow the existing code style (no trailing whitespace, standard Java formatting).
- Use conventional commits for your commit messages:
  - `feat:` new feature
  - `fix:` bug fix
  - `docs:` documentation only
  - `refactor:` code change that neither fixes a bug nor adds a feature
  - `test:` adding or correcting tests

## Code style

- Java 21, records preferred for value objects
- No Lombok
- Minimal comments — code should be self-explanatory
- Prefer immutable data structures

## Reporting bugs

Open a GitHub issue with a minimal reproducible example.
