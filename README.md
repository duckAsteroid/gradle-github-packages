# gradle-github-packages

Plugins for Gradle to make it easier to configure GitHub Packages as Maven repositories in both settings and build files.

## Features

- 🔐 **Flexible credential resolution** with three-tier fallback chain, named profiles, and configurable env var names
- 📦 **Build script support** via `repositories { gitHubPackages { ... } }`
- ⚙️ **Settings plugin** for `pluginManagement.repositories` and `dependencyResolutionManagement.repositories`
- 📝 **Zero configuration** - automatically uses your GitHub credentials
- 🧪 **Fully tested** with comprehensive functional test coverage

## Build Script Usage

```groovy
plugins {
    id 'io.github.duckasteroid.github-packages'
}

repositories {
    gitHubPackages {
        owner = "duckAsteroid"
        repo = "testing"
        // Optional: pick a named credential profile (see "Named Credential Profiles" below)
        // profile = "personal"
        // Optional: read the shared credential from differently-named env vars (see "Custom
        // Environment Variable Names" below)
        // userEnvVar = "MY_USER_ENV_VAR"
        // tokenEnvVar = "MY_TOKEN_ENV_VAR"
        // Optional: override credentials outright (defaults to credential resolution chain)
        // username = "my-user"
        // token = "ghp_xxx"
    }
}
```

## Settings / Plugin Management Usage

Due to Gradle's ordering constraint (`pluginManagement {}` must be the first block), the settings plugin cannot inject the `gitHubPackages` DSL method into `pluginManagement.repositories` when declared in the same file.

Use the settings plugin with its extension form instead:

```groovy
plugins {
    id 'io.github.duckasteroid.github-packages-settings'
}

githubPackages {
    owner = "duckAsteroid"
    repository = "testing"
    // Optional: pick a named credential profile (see "Named Credential Profiles" below)
    // profile = "personal"
    // Optional: read the shared credential from differently-named env vars (see "Custom
    // Environment Variable Names" below)
    // userEnvVar = "MY_USER_ENV_VAR"
    // tokenEnvVar = "MY_TOKEN_ENV_VAR"
    // Optional: override credentials outright
    // username = "my-user"
    // token = "ghp_xxx"
}
```

This configures GitHub Packages for both:
- `pluginManagement.repositories` (for plugin resolution)
- `dependencyResolutionManagement.repositories` (for dependency resolution)

## Credential Resolution

Credentials are resolved using a three-tier fallback chain. The first available source wins:

### Tier 1: Gradle Properties (Highest Priority)
```properties
# In gradle.properties (project or ~/.gradle/gradle.properties)
gpr.user=your-github-username
gpr.key=your-github-token
```

### Tier 2: Shared Credential Environment Variables
```bash
export GH_PACKAGES_READ_USER=your-github-username
export GH_PACKAGES_READ_TOKEN=your-github-token
```

These are useful for a credential shared across multiple repos/builds - e.g. a bot account or org
token - without storing a full PAT as a per-repository secret. The variable names are just a
convention; the plugin doesn't inspect or enforce what scopes the token actually has, and the
names can be overridden (see "Custom Environment Variable Names" below).

### Tier 3: Standard GitHub Actions Environment Variables (Lowest Priority)
```bash
export GITHUB_ACTOR=your-github-username
export GITHUB_TOKEN=your-github-token
```

`GITHUB_ACTOR` is automatically injected into every GitHub Actions step with no workflow wiring.
`GITHUB_TOKEN`, however, is *not* in GitHub's own list of default environment variables — it's
auto-generated per workflow run, but still has to be explicitly forwarded into the job/step
environment before this plugin's `System.getenv("GITHUB_TOKEN")` lookup will see it:

```yaml
jobs:
  build:
    env:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    steps:
      - run: ./gradlew build
```

Without that `env:` line, Tier 3 silently falls through (empty `GITHUB_TOKEN`) and, absent Tier
1/2 credentials, resolution against GitHub Packages fails in CI.

## Precedence Example

If you have:
- `gpr.user=alice` in gradle.properties
- `GH_PACKAGES_READ_USER=charlie` in environment
- `GITHUB_ACTOR=bob` in environment

The plugin will use `alice` (from Tier 1), ignoring the environment variables.

Without `gpr.user`, it will use `charlie` (Tier 2) before `bob` (Tier 3).

## Named Credential Profiles

`gpr.user` / `gpr.key` are global - they win over everything for every `gitHubPackages { }` /
`githubPackages { }` block in every project. If `~/.gradle/gradle.properties` holds your work
credentials, set `profile` to use a different identity (e.g. a personal GitHub account) for a
specific repo, without overriding `username`/`token` explicitly:

```groovy
repositories {
    gitHubPackages {
        owner = "personal-org"
        repo = "personal-lib"
        profile = "personal"
    }
}
```

```properties
# ~/.gradle/gradle.properties
gpr.user=work-username
gpr.key=ghp_work_token
gpr.personal.user=personal-username
gpr.personal.key=ghp_personal_token
```

When `profile` is set, credentials are resolved as:

1. `gpr.<profile>.user` / `gpr.<profile>.key`
2. `GH_PACKAGES_READ_USER` / `GH_PACKAGES_READ_TOKEN`
3. `GITHUB_ACTOR` / `GITHUB_TOKEN`

Note that tier 1 does *not* fall back to the unqualified `gpr.user` / `gpr.key` - a mistyped
profile name falls through to tiers 2 and 3 instead of silently picking up the wrong identity.
This fallthrough is also what keeps a `profile` set in a committed build script working in CI:
there's no `gpr.<profile>.*` there, so it resolves from `GITHUB_ACTOR`/`GITHUB_TOKEN` as usual.

When `profile` is unset, behavior is unchanged (the three-tier chain above, starting from the
unqualified `gpr.user` / `gpr.key`).

## Custom Environment Variable Names

If your CI secrets already use different variable names for the tier 2 shared credential than
`GH_PACKAGES_READ_USER` / `GH_PACKAGES_READ_TOKEN`, set `userEnvVar` / `tokenEnvVar` instead of
renaming your secrets:

```groovy
repositories {
    gitHubPackages {
        owner = "duckAsteroid"
        repo = "testing"
        userEnvVar = "MY_USER_ENV_VAR"
        tokenEnvVar = "MY_TOKEN_ENV_VAR"
    }
}
```

```groovy
githubPackages {
    owner = "duckAsteroid"
    repository = "testing"
    userEnvVar = "MY_USER_ENV_VAR"
    tokenEnvVar = "MY_TOKEN_ENV_VAR"
}
```

Only the tier 2 variable *name* changes - tier 1 (`gpr.user`/`gpr.key`) still wins when present,
and tier 3 (`GITHUB_ACTOR`/`GITHUB_TOKEN`) is still the fallback if `MY_USER_ENV_VAR` /
`MY_TOKEN_ENV_VAR` are unset. `userEnvVar`/`tokenEnvVar` can be combined with `profile` - they're
independent settings.

## Common Scenarios

### Local Development with PAT
Store your Personal Access Token in `~/.gradle/gradle.properties`:
```properties
gpr.user=your-username
gpr.key=ghp_your_personal_access_token
```

### GitHub Actions CI/CD
`GITHUB_ACTOR` needs no extra configuration, but `GITHUB_TOKEN` must still be explicitly forwarded
into the job/step environment, same as any other secret:
```yaml
jobs:
  build:
    env:
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    steps:
      - run: ./gradlew build
```
If `GH_PACKAGES_READ_*` is also set, the plugin prefers `GH_PACKAGES_READ_*`.

### Organization-Wide Read Access
Use bot accounts or organization tokens with GitHub's `GH_PACKAGES_READ_*` variables to provide read access across multiple repositories without storing secrets:
```yaml
env:
  GH_PACKAGES_READ_USER: github-actions-bot
  GH_PACKAGES_READ_TOKEN: ${{ secrets.PACKAGES_READ_TOKEN }}
```
