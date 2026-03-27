---
name: maven-dependency-management
description: Audit Maven dependencies and plugins for outdated versions and unused declarations. Use when user asks "are dependencies up to date", "check for unused dependencies", "update pom.xml", or "dependency audit".
---

# Maven Dependency Management Skill

Audit a Maven project for outdated dependencies, outdated plugins, unused declared dependencies, and undeclared used dependencies.

## When to Use
- User asks "are my dependencies up to date?" or "update dependencies"
- User asks "check for unused dependencies" or "clean up pom.xml"
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

### Recommendations
1. [Actionable items in priority order]
```

---

## Key Rules

- **Never remove a Spring Boot starter without understanding its auto-configuration role.**
- **Always run `mvn test` after removing a dependency** to confirm nothing breaks.
- **For major version bumps**, check the library's migration guide before updating.
- **Prefer BOM-managed versions** (via `spring-boot-starter-parent`) over pinning explicit versions — less to maintain.
- **Used undeclared dependencies are a risk**: if the direct dependency drops the transitive one, your build silently breaks. Always declare what you directly use.
