# gradle-github-packages
Plugins for Gradle to make it easier to configure GH packages as maven repositories; in both settings and build files.

## Build script usage

```groovy
plugins {
    id 'io.github.duckasteroid.github-packages'
}

repositories {
    gitHubPackages {
        owner = "duckAsteroid"
        repo = "testing"
        // optional overrides; default from gpr.user/gpr.key then env vars
        // username = "my-user"
        // token = "ghp_xxx"
    }
}
```

## Settings / plugin management usage

Due to Gradle ordering rules, `pluginManagement {}` must be the first block in `settings.gradle`, so
`pluginManagement.repositories.gitHubPackages { ... }` cannot be provided by a settings plugin declared later
in the same file.

Use the settings plugin's extension form instead:

```groovy
plugins {
    id 'io.github.duckasteroid.github-packages-settings'
}

githubPackages {
    owner = "duckAsteroid"
    repository = "testing"
    // optional username/token overrides
}
```

This configures GitHub Packages for `pluginManagement.repositories` and `dependencyResolutionManagement.repositories`.

Credential lookup order:
1. `gpr.user` / `gpr.key` in `gradle.properties` (project or `~/.gradle/gradle.properties`)
2. `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables
