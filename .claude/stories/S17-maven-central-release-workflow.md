# S17 — Maven Central: GitHub Actions Release Workflow

## Goal
Add a GitHub Actions release workflow that signs and publishes all Eventus artifacts to Maven Central (via OSSRH) when a version tag is pushed.

## Acceptance Criteria
- [ ] `.github/workflows/release.yml` exists and triggers on `v*` tags
- [ ] Workflow uses Java 21 + Maven
- [ ] GPG key imported from GitHub secret `GPG_PRIVATE_KEY` + passphrase `GPG_PASSPHRASE`
- [ ] OSSRH credentials read from secrets `OSSRH_USERNAME` + `OSSRH_TOKEN`
- [ ] `mvn deploy -Prelease` runs with signing enabled
- [ ] Workflow uploads to OSSRH staging and auto-closes + promotes via Nexus Staging Plugin
- [ ] Existing CI workflow (`.github/workflows/ci.yml`) is unchanged
- [ ] README documents the release process (required secrets + how to trigger)

## Workflow File

```yaml
# .github/workflows/release.yml
name: Release to Maven Central

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
          server-id: ossrh
          server-username: MAVEN_USERNAME
          server-password: MAVEN_PASSWORD

      - name: Import GPG key
        run: |
          echo "${{ secrets.GPG_PRIVATE_KEY }}" | gpg --batch --import
          gpg --list-secret-keys

      - name: Configure GPG passphrase
        run: |
          echo "allow-loopback-pinentry" >> ~/.gnupg/gpg-agent.conf
          gpgconf --kill gpg-agent

      - name: Build and deploy to Maven Central
        env:
          MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.OSSRH_TOKEN }}
          GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
        run: |
          mvn --batch-mode deploy -Prelease \
            -Dgpg.passphrase=${GPG_PASSPHRASE}
```

## Nexus Staging Plugin (add to release profile in pom.xml)

```xml
<plugin>
  <groupId>org.sonatype.plugins</groupId>
  <artifactId>nexus-staging-maven-plugin</artifactId>
  <version>1.6.13</version>
  <extensions>true</extensions>
  <configuration>
    <serverId>ossrh</serverId>
    <nexusUrl>https://s01.oss.sonatype.org/</nexusUrl>
    <autoReleaseAfterClose>true</autoReleaseAfterClose>
  </configuration>
</plugin>
```

## README Section to Add

```markdown
## Releasing to Maven Central

### Prerequisites

Register the following secrets in GitHub → Settings → Secrets → Actions:

| Secret | Description |
|---|---|
| `GPG_PRIVATE_KEY` | ASCII-armored GPG private key (`gpg --export-secret-keys --armor <KEY_ID>`) |
| `GPG_PASSPHRASE` | Passphrase for the GPG key |
| `OSSRH_USERNAME` | Sonatype OSSRH username (or user token) |
| `OSSRH_TOKEN` | Sonatype OSSRH password (or token) |

### Triggering a Release

```bash
# Bump version in root pom.xml and all module poms
mvn versions:set -DnewVersion=0.2.0
git add -A && git commit -m "chore: release 0.2.0"
git tag v0.2.0
git push origin main --tags
```

The GitHub Actions workflow picks up the tag and deploys to Maven Central automatically.
```

## Manual One-Time Setup (document but don't automate)

Document in README:
1. Create Sonatype OSSRH account at `issues.sonatype.org`
2. Open a ticket to register group ID `io.eventus` pointing to `github.com/rafaelmaia/eventus`
3. Generate a user token in OSSRH Nexus UI and store as GitHub secrets
4. Publish a GPG key to `keyserver.ubuntu.com`

## Done When
- `.github/workflows/release.yml` exists with correct triggers and steps
- README documents the release process with required secrets
- Nexus staging plugin added to release profile in root pom.xml
- Dry-run tested: `mvn deploy -Prelease -Dgpg.skip=true -DskipTests -DaltDeploymentRepository=local::default::file:///tmp/test-deploy` succeeds locally
