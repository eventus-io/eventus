# S09 — GitHub Repository & First Push

## Goal
Create the public GitHub repository and push the complete v0.1 codebase.
This is the final story in the v0.1 milestone.

## Prerequisites
- Stories S01–S08 are complete
- `mvn verify` passes from the root directory (all tests green)
- `gh` CLI is authenticated (`gh auth status` shows logged in)

## Acceptance Criteria
- [ ] GitHub repository exists at `github.com/<your-username>/eventus`
- [ ] Repository is public
- [ ] Repository description: "Event topology, made visible."
- [ ] Repository topics set: `java`, `spring-boot`, `spring-modulith`, `event-driven`, `knowledge-graph`, `observability`
- [ ] All code is on the `main` branch
- [ ] Initial commit uses conventional commit format
- [ ] GitHub Actions workflow runs and passes on the first push
- [ ] README renders correctly on the repository homepage

## Steps

### 1. Final verification
```bash
mvn verify
# Must print BUILD SUCCESS before proceeding
```

### 2. Initialise git (if not already done)
```bash
git init
git checkout -b main
```

### 3. Create .gitignore (if not already done by S01)
Verify `.gitignore` covers:
```
target/
*.class
*.jar
*.war
.idea/
*.iml
.vscode/
.DS_Store
```

### 4. Stage and commit
```bash
git add .
git commit -m "feat: initial v0.1 — EventGraphExtractor, InMemoryBackend, SpringModulithExtractor, actuator endpoints"
```

### 5. Create GitHub repository and push
```bash
gh repo create eventus \
  --public \
  --description "Event topology, made visible." \
  --source=. \
  --remote=origin \
  --push
```

If `--source` flag is not available in your gh version, use:
```bash
gh repo create eventus --public --description "Event topology, made visible."
git remote add origin https://github.com/$(gh api user --jq .login)/eventus.git
git push -u origin main
```

### 6. Set repository topics
```bash
gh repo edit eventus \
  --add-topic java \
  --add-topic spring-boot \
  --add-topic spring-modulith \
  --add-topic event-driven \
  --add-topic knowledge-graph \
  --add-topic observability
```

### 7. Verify
```bash
# Print the repo URL
gh repo view eventus --json url --jq .url

# Check Actions ran
gh run list --repo $(gh api user --jq .login)/eventus
```

### 8. Print summary
Output the following when done:
- Repository URL
- Number of files committed
- GitHub Actions status (pass/fail)
- Next milestone: v0.2 (embedded React UI)

## Done When
The repository is live, public, and the GitHub Actions build badge on the
README shows green. Share the URL.
