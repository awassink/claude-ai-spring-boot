---
name: maven-dependency-management
description: Audit Maven dependencies and plugins for outdated versions, unused declarations, and known CVE vulnerabilities. Use when user asks "are dependencies up to date", "check for unused dependencies", "update pom.xml", "dependency audit", "CVE scan", or "security vulnerabilities".
---

# Maven Dependency Management Skill

Audit a Maven project for outdated dependencies, outdated plugins, unused declared dependencies, undeclared used dependencies, and known CVE security vulnerabilities.

## When to Use
- User asks "are my dependencies up to date?" or "update dependencies"
- User asks "check for unused dependencies" or "clean up pom.xml"
- User asks "scan for CVEs", "check for vulnerabilities", or "security audit"
- Before a release or security review
- After adding new features (new transitive deps may have crept in)

## Execution Steps

Run the following commands in order. Work from the project root containing `pom.xml`. Use `mvn` (or `./mvnw` if a Maven wrapper is present).

### Step 1 — Check for outdated dependencies

```bash
mvn versions:display-dependency-updates
```

Collect every line that shows `... -> X.Y.Z`. These are dependencies with newer versions available.

**What to look for:**
- Major version bumps → flag for manual review (breaking changes possible)
- Minor/patch bumps → generally safe to update
- `-SNAPSHOT` versions in non-snapshot projects → should be replaced with releases

### Step 2 — Check for outdated plugins

```bash
mvn versions:display-plugin-updates
```

Collect every plugin that has a newer version. Pay special attention to:
- `maven-compiler-plugin` — affects Java source/target compatibility
- `maven-surefire-plugin` — affects test execution
- `spring-boot-maven-plugin` — must stay aligned with Spring Boot parent version
- `jacoco-maven-plugin`, `maven-enforcer-plugin` — keep current for accurate analysis

### Step 3 — Analyze unused and undeclared dependencies

```bash
mvn dependency:analyze
```

This produces two categories:

| Category | Meaning | Action |
|----------|---------|--------|
| **Used undeclared** | Your code uses it but it arrives transitively — fragile | Declare it explicitly in `pom.xml` |
| **Unused declared** | Declared in `pom.xml` but no bytecode reference found | Investigate before removing (see rules below) |

**Rules for "Unused declared":**
- `spring-boot-starter-*` — always keep; starters configure auto-configuration even without direct class references
- Test frameworks (`spring-boot-starter-test`, `spring-security-test`) — keep; missing tests ≠ unused dependency
- Annotation processors, agents, JDBC drivers, Flyway — keep; no direct bytecode reference by design
- Logging backends (`logback-classic`) — keep; loaded via SPI
- Everything else with no clear reason → candidate for removal; verify by removing and running `mvn test`

### Step 4 — Inspect the full dependency tree (when needed)

```bash
mvn dependency:tree
```

Use this to understand *why* a transitive dependency is present before updating or removing a direct one.

To trace a specific artifact:
```bash
mvn dependency:tree -Dincludes=<groupId>:<artifactId>
# Example:
mvn dependency:tree -Dincludes=org.slf4j:slf4j-api
```

### Step 5 — Inspect the effective POM (when needed)

```bash
mvn help:effective-pom
```

Use this to see the fully resolved versions after parent BOM inheritance — useful when a version in `pom.xml` is managed by `spring-boot-starter-parent` and you want to confirm the resolved value.

---

### Step 6 — Scan for known CVE vulnerabilities (OWASP Dependency-Check)

Uses the [OWASP Dependency-Check plugin](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/) to cross-reference all resolved dependencies against the NVD (National Vulnerability Database).

#### Prerequisites — NVD API key

Since version 8.0, OWASP Dependency-Check requires an NVD API key to initialize its local H2 database. **The scan cannot run at all without it** — even in OSS Index-only mode, the local database schema must be populated first. Without a key the NVD API returns 403 and the plugin aborts with `NoDataException: No documents exist`.

1. Register for free at https://nvd.nist.gov/developers/request-an-api-key
2. Pass the key at scan time or store it in `~/.m2/settings.xml`:

```xml
<!-- ~/.m2/settings.xml -->
<settings>
  <profiles>
    <profile>
      <id>nvd</id>
      <properties>
        <nvd.api.key>YOUR_KEY_HERE</nvd.api.key>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>nvd</activeProfile>
  </activeProfiles>
</settings>
```

#### Run the scan

```bash
# With NVD API key from settings.xml
mvn org.owasp:dependency-check-maven:9.2.0:check

# With NVD API key passed inline
mvn org.owasp:dependency-check-maven:9.2.0:check -DnvdApiKey=YOUR_KEY_HERE

# Fail the build if any CVE with CVSS score >= 7 (HIGH) is found
mvn org.owasp:dependency-check-maven:9.2.0:check -DfailBuildOnCVSS=7

# Skip test-scoped dependencies (reduces noise)
mvn org.owasp:dependency-check-maven:9.2.0:check -DskipTestScope=true
```

The HTML report is written to `target/dependency-check-report.html`. Open it in a browser for full details including CVE descriptions, affected versions, and remediation links.

#### Interpreting results

| CVSS Score | Severity | Action |
|------------|----------|--------|
| 9.0 – 10.0 | **Critical** | Block release. Update immediately or apply suppression with documented justification. |
| 7.0 – 8.9  | **High**     | Fix before next release. Escalate if exploitable remotely. |
| 4.0 – 6.9  | **Medium**   | Plan fix within sprint. Assess exploitability in your context. |
| 0.1 – 3.9  | **Low**      | Track and fix opportunistically. |

**Key questions to ask for each finding:**
1. Is the vulnerable code path reachable in this application?
2. Is the vulnerability in the direct dependency or a transitive one?
3. Is a patched version available? Does upgrading break anything?
4. Is this a false positive (e.g., a shaded/relocated class)?

#### Suppressing false positives

If a CVE is confirmed as a false positive or accepted risk, suppress it with a `dependency-suppression.xml` file rather than ignoring it silently:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
  <suppress>
    <notes>False positive: CVE-XXXX-YYYY affects the CLI tool, not the library API we use.</notes>
    <packageUrl regex="true">^pkg:maven/org\.example/some\-lib@.*$</packageUrl>
    <cve>CVE-XXXX-YYYY</cve>
  </suppress>
</suppressions>
```

Pass it to the scan:
```bash
mvn org.owasp:dependency-check-maven:9.2.0:check \
  -DsuppressionFile=dependency-suppression.xml
```

Every suppression **must** have a `<notes>` element explaining why it is safe to suppress.

#### Adding OWASP scan to pom.xml (optional — for CI integration)

To run the CVE check automatically during `mvn verify`:

```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>9.2.0</version>
  <configuration>
    <nvdApiKey>${nvd.api.key}</nvdApiKey>
    <failBuildOnCVSS>7</failBuildOnCVSS>
    <skipTestScope>true</skipTestScope>
    <suppressionFile>dependency-suppression.xml</suppressionFile>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Store `nvd.api.key` in CI secrets and pass it as `-Dnvd.api.key=$NVD_API_KEY`. Do **not** hardcode it in `pom.xml`.

---

## Output Format

```markdown
## Maven Dependency Audit

### Outdated Dependencies
| Dependency | Current | Latest | Risk | Recommendation |
|------------|---------|--------|------|----------------|
| org.example:foo | 2.1.0 | 3.0.0 | Major — review changelog | Update manually after review |
| org.example:bar | 1.4.2 | 1.4.5 | Patch — safe | Update |

### Outdated Plugins
| Plugin | Current | Latest | Recommendation |
|--------|---------|--------|----------------|
| maven-surefire-plugin | 3.1.2 | 3.2.5 | Update |

### Unused Declared Dependencies
| Dependency | Verdict | Reason |
|------------|---------|--------|
| spring-boot-starter-web | Keep | Auto-configuration — no direct refs by design |
| com.example:legacy-util | Remove candidate | No references found, no SPI role |

### Used Undeclared Dependencies
| Dependency | Transitive via | Action |
|------------|----------------|--------|
| org.springframework:spring-beans | spring-webmvc | Declare explicitly |

### CVE Vulnerabilities
| CVE | Severity (CVSS) | Dependency | Affected Version | Fix Version | Action |
|-----|-----------------|------------|-----------------|-------------|--------|
| CVE-XXXX-YYYY | Critical (9.8) | org.example:foo:1.2.3 | < 1.2.4 | 1.2.4 | Update immediately |
| CVE-XXXX-ZZZZ | Medium (5.3) | org.example:bar:2.0.0 | all | None available | Suppress with justification |

### Recommendations
1. [Actionable items in priority order — CVE fixes first, then updates, then cleanup]
```

---

## Key Rules

- **Never remove a Spring Boot starter without understanding its auto-configuration role.**
- **Always run `mvn test` after removing a dependency** to confirm nothing breaks.
- **For major version bumps**, check the library's migration guide before updating.
- **Prefer BOM-managed versions** (via `spring-boot-starter-parent`) over pinning explicit versions — less to maintain.
- **Used undeclared dependencies are a risk**: if the direct dependency drops the transitive one, your build silently breaks. Always declare what you directly use.
- **CVE findings are ordered by CVSS score** — address Critical and High before any release.
- **Never suppress a CVE without a `<notes>` element** documenting why it is safe to suppress.
- **A CVE in a transitive dep is still your problem** — upgrade the direct dependency that pulls it in, or exclude and re-declare the transitive dep at a patched version.
- **The NVD API key is required** — without it the plugin cannot initialize its local database and aborts with `NoDataException`. Get a free key at https://nvd.nist.gov/developers/request-an-api-key. Store it in `~/.m2/settings.xml` locally and as a CI secret in pipelines. Never hardcode it in `pom.xml`.
