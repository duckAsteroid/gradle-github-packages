# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A Gradle plugin (published to the Gradle Plugin Portal as `io.github.duckasteroid.github-packages` and `io.github.duckasteroid.github-packages-settings`) that configures GitHub Packages as an authenticated Maven repository, in both build scripts and `settings.gradle`. Java 17, built with Gradle itself (`java-gradle-plugin`).

## Common commands

```bash
./gradlew clean check          # full build: unit tests + functional tests
./gradlew test                 # unit tests only (src/test)
./gradlew functionalTest       # functional tests only (src/functionalTest, uses Gradle TestKit)
./gradlew test --tests "GithubPackagesPluginTest.pluginRegistersExtension"
./gradlew functionalTest --tests "GithubPackagesPluginFunctionalTest.pluginAppliesAndAddsRepository"
```

There is no separate lint/format task configured. `check` depends on `functionalTest` (wired in `build.gradle`), so a single `./gradlew check` covers both suites — this is what CI (`.github/workflows/build.yml`) runs.

Versioning is derived from git tags via the `axion-release` plugin (`version = scmVersion.version` in `build.gradle`) — there is no hardcoded version to bump. Publishing (`publish`, `publishPlugins`) only happens in CI on `v*` tags pushed to `main` (`.github/workflows/publish.yml`); don't run publish tasks locally.

## Architecture

Three source sets: `src/main` (plugin code), `src/test` (fast unit tests using `ProjectBuilder`), `src/functionalTest` (black-box tests using `GradleRunner`/TestKit that write real `build.gradle`/`settings.gradle` files to a temp dir and assert on build output — this is where most behavior is actually verified, since the plugins' effects are only observable through Gradle's repository/model APIs).

Two plugin entry points share the same credential/repository machinery:

- **`GithubPackagesPlugin`** (`io.github.duckasteroid.github-packages`) — applies to a *project* build script. Registers the `githubPackages { owner; repository }` extension and, in `project.afterEvaluate`, adds a Maven repo to `project.repositories` and (if `maven-publish` is applied) to `publishing.repositories`.
- **`GithubPackagesSettingsPlugin`** (`io.github.duckasteroid.github-packages-settings`) — applies to `settings.gradle`. Registers the same `githubPackages` extension on `Settings`, and in `gradle.settingsEvaluated` adds the repo to both `pluginManagement.repositories` and `dependencyResolutionManagement.repositories`.

Both plugins also inject a `gitHubPackages { owner; repo; ... }` closure/method (via `ExtraPropertiesExtension`, key `"gitHubPackages"`) that can be called *inside* a `repositories { }` block — this is `GithubPackagesRepositoryDsl`, which supports both a Groovy `Closure` and a Gradle `Action` entry point and works against any `RepositoryHandler` (project repos, publishing repos, or pluginManagement repos). Note the naming split: the block-scoped DSL uses `owner`/`repo`, while the `githubPackages` extension uses `owner`/`repository` — these are two different config surfaces, not a typo.

Gradle's `pluginManagement { }` block must be the first statement in `settings.gradle`, so `GithubPackagesSettingsPlugin`'s injected `gitHubPackages` DSL method cannot be used inside `pluginManagement.repositories { }` in the *same* settings file where the settings plugin itself is applied via `plugins { }` (see the functional test `pluginManagementDslCannotUseSettingsPluginDefinedMethodsInSameSettingsFile`, which asserts this fails). The documented workaround is to configure the top-level `githubPackages { owner; repository }` extension instead, which the settings plugin applies to `pluginManagement.repositories` itself after settings evaluation.

Credential resolution (`CredentialProviders`, keys centralized in `CredentialKeys`) is a three-tier `Provider.orElse()` chain, same for username and token, first available value wins:

1. Gradle property `gpr.user` / `gpr.key` (from `gradle.properties`, project or `~/.gradle`)
2. Env var `GH_PACKAGES_READ_USER` / `GH_PACKAGES_READ_TOKEN` (read-only org-wide credentials)
3. Env var `GITHUB_ACTOR` / `GITHUB_TOKEN` (GitHub Actions default)

This chain is the source of truth for precedence — reflected in `README.md` and in `CredentialProviders`. Some older Javadoc comments elsewhere in the plugin classes state tiers 2 and 3 in the opposite order; if you touch credential resolution, fix those stale comments too rather than trusting them.

`GithubPackagesExtension` is shared by both plugins (same class, applied to either `Project` or `Settings` extensions) and computes `mavenUrl()` as `https://maven.pkg.github.com/{owner}/{repository}`; explicit `username`/`token` set on the extension or DSL spec override the resolved-credential convention.
