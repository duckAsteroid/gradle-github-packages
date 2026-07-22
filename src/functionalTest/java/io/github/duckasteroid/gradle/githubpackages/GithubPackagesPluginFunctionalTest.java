package io.github.duckasteroid.gradle.githubpackages;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for {@link GithubPackagesPlugin} using Gradle TestKit.
 */
class GithubPackagesPluginFunctionalTest {

    @TempDir
    File projectDir;

    private File buildFile() {
        return new File(projectDir, "build.gradle");
    }

    private File settingsFile() {
        return new File(projectDir, "settings.gradle");
    }

    private File gradlePropertiesFile() {
        return new File(projectDir, "gradle.properties");
    }

    @Test
    void pluginAppliesAndAddsRepository() throws IOException {
        // language=groovy
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");

        // language=groovy
        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                    username   = 'user'
                    token      = 'ghp_dummy'
                }

                tasks.register('printRepos') {
                    doLast {
                        repositories.each { println 'REPO: ' + it.name }
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepos", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO: GitHubPackages-test-repo"),
                "Expected repository 'GitHubPackages-test-repo' to be registered.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepos").getOutcome());
    }

    @Test
    void pluginConfiguresPublishingRepositoryWhenMavenPublishIsPresent() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                    id 'maven-publish'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                    username   = 'user'
                    token      = 'ghp_dummy'
                }

                tasks.register('printPublishRepos') {
                    doLast {
                        publishing.repositories.each { println 'PUB_REPO: ' + it.name }
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printPublishRepos", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("PUB_REPO: GitHubPackages-test-repo"),
                "Expected publishing repository 'GitHubPackages-test-repo' to be registered.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printPublishRepos").getOutcome());
    }

    @Test
    void usesEnvironmentVariablesWhenPropertiesAreNotSet() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GITHUB_ACTOR", "env-user",
                        "GITHUB_TOKEN", "env-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: env-user"),
                "Expected username from GITHUB_ACTOR when gpr.user is not set.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: env-token"),
                "Expected token from GITHUB_TOKEN when gpr.key is not set.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void gradlePropertiesTakePrecedenceOverEnvironmentVariables() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");
        Files.writeString(gradlePropertiesFile().toPath(), """
                gpr.user=prop-user
                gpr.key=prop-token
                """);

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GITHUB_ACTOR", "env-user",
                        "GITHUB_TOKEN", "env-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: prop-user"),
                "Expected gpr.user to take precedence over GITHUB_ACTOR.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: prop-token"),
                "Expected gpr.key to take precedence over GITHUB_TOKEN.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void settingsPluginConfiguresPluginManagementRepository() throws IOException {
        Files.writeString(settingsFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages-settings'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                    username   = 'user'
                    token      = 'ghp_dummy'
                }

                gradle.settingsEvaluated { evaluatedSettings ->
                    evaluatedSettings.pluginManagement.repositories.each { repo ->
                        println 'PM_REPO: ' + repo.name
                    }
                }
                """);

        Files.writeString(buildFile().toPath(), """
                tasks.register('verifySettingsPlugin') {
                    doLast {
                        println 'SETTINGS_PLUGIN_OK'
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("verifySettingsPlugin", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("PM_REPO: GitHubPackages-test-repo"),
                "Expected pluginManagement repository 'GitHubPackages-test-repo' to be registered.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifySettingsPlugin").getOutcome());
    }

    @Test
    void settingsPluginPreservesDefaultGradlePluginPortal() throws IOException {
        Files.writeString(settingsFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages-settings'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                    username   = 'user'
                    token      = 'ghp_dummy'
                }

                gradle.settingsEvaluated { evaluatedSettings ->
                    evaluatedSettings.pluginManagement.repositories.each { repo ->
                        println 'PM_REPO: ' + repo.name
                    }
                }
                """);

        Files.writeString(buildFile().toPath(), """
                tasks.register('verifyPluginPortalPreserved') {
                    doLast {
                        println 'PLUGIN_PORTAL_PRESERVED_OK'
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("verifyPluginPortalPreserved", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("PM_REPO: Gradle Central Plugin Repository"),
                "Expected the default Gradle Plugin Portal to remain in pluginManagement.repositories "
                        + "after the settings plugin adds its own repository.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("PM_REPO: GitHubPackages-test-repo"),
                "Expected pluginManagement repository 'GitHubPackages-test-repo' to still be registered.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifyPluginPortalPreserved").getOutcome());
    }

    @Test
    void repositoriesBlockCanDeclareGitHubPackagesRepository() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                repositories {
                    gitHubPackages {
                        owner = 'duckAsteroid'
                        repo = 'testing'
                        username = 'user'
                        token = 'ghp_dummy'
                    }
                }

                tasks.register('printRepos') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-testing')
                        println 'REPO_NAME: ' + repo.name
                        println 'REPO_URL: ' + repo.url
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepos", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_NAME: GitHubPackages-testing"),
                "Expected repositories.gitHubPackages to register repository name.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_URL: https://maven.pkg.github.com/duckAsteroid/testing"),
                "Expected repositories.gitHubPackages to register the GitHub Packages URL.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepos").getOutcome());
    }

    @Test
    void gitHubPackagesInsidePublishingRepositoriesTargetsPublishingRepositories() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                    id 'maven-publish'
                }

                publishing {
                    repositories {
                        gitHubPackages {
                            owner = 'duckAsteroid'
                            repo = 'publish-target'
                            username = 'user'
                            token = 'ghp_dummy'
                        }
                    }
                }

                tasks.register('printRepos') {
                    doLast {
                        repositories.each { println 'PROJECT_REPO: ' + it.name }
                        publishing.repositories.each { println 'PUBLISH_REPO: ' + it.name }
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepos", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("PUBLISH_REPO: GitHubPackages-publish-target"),
                "Expected gitHubPackages inside publishing.repositories to register there.\n" + result.getOutput());
        assertTrue(!result.getOutput().contains("PROJECT_REPO: GitHubPackages-publish-target"),
                "Did not expect gitHubPackages inside publishing.repositories to leak into project.repositories.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepos").getOutcome());
    }

    @Test
    void pluginManagementDslCannotUseSettingsPluginDefinedMethodsInSameSettingsFile() throws IOException {
        Files.writeString(settingsFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages-settings'
                }

                pluginManagement {
                    repositories {
                        gitHubPackages {
                            owner = 'duckAsteroid'
                            repo = 'testing'
                        }
                    }
                }
                """);

        Files.writeString(buildFile().toPath(), """
                tasks.register('verifyPluginManagementDsl') {
                    doLast {
                        println 'PLUGIN_MANAGEMENT_DSL_OK'
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("verifyPluginManagementDsl", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail();

        assertTrue(result.getOutput().contains("The pluginManagement {} block must appear before any other statements in the script."),
                "Expected Gradle to reject pluginManagement after plugins block; this is a Gradle ordering rule.\n" + result.getOutput());
    }

    @Test
    void settingsPluginHappyPathWithOwnerAndRepositoryOnly() throws IOException {
        Files.writeString(settingsFile().toPath(), """
                plugins {
                  id 'io.github.duckasteroid.github-packages-settings'
                }

                githubPackages {
                  owner = "duckAsteroid"
                  repository = "testing"
                }

                gradle.settingsEvaluated { evaluatedSettings ->
                    evaluatedSettings.pluginManagement.repositories.each { repo ->
                        println 'PM_REPO: ' + repo.name
                        println 'PM_REPO_URL: ' + repo.url
                    }
                    evaluatedSettings.dependencyResolutionManagement.repositories.each { repo ->
                        println 'DRM_REPO: ' + repo.name
                        println 'DRM_REPO_URL: ' + repo.url
                    }
                }
                """);

        Files.writeString(buildFile().toPath(), """
                tasks.register('verifySettingsHappyPath') {
                    doLast {
                        println 'SETTINGS_HAPPY_PATH_OK'
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("verifySettingsHappyPath", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("PM_REPO: GitHubPackages-testing"),
                "Expected settings plugin to register pluginManagement repository name.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("PM_REPO_URL: https://maven.pkg.github.com/duckAsteroid/testing"),
                "Expected settings plugin to register pluginManagement repository URL.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("DRM_REPO: GitHubPackages-testing"),
                "Expected settings plugin to register dependencyResolutionManagement repository name.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("DRM_REPO_URL: https://maven.pkg.github.com/duckAsteroid/testing"),
                "Expected settings plugin to register dependencyResolutionManagement repository URL.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifySettingsHappyPath").getOutcome());
    }

    @Test
    void usesReadPackageEnvironmentVariablesWhenPrimaryCredentialsAreNotSet() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GH_PACKAGES_READ_USER", "read-user",
                        "GH_PACKAGES_READ_TOKEN", "read-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: read-user"),
                "Expected username from GH_PACKAGES_READ_USER when higher precedence sources are absent.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: read-token"),
                "Expected token from GH_PACKAGES_READ_TOKEN when higher precedence sources are absent.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void readPackageEnvironmentVariablesTakePrecedenceOverGithubActorAndToken() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GITHUB_ACTOR", "env-user",
                        "GITHUB_TOKEN", "env-token",
                        "GH_PACKAGES_READ_USER", "read-user",
                        "GH_PACKAGES_READ_TOKEN", "read-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: read-user"),
                "Expected GH_PACKAGES_READ_USER to take precedence over GITHUB_ACTOR.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: read-token"),
                "Expected GH_PACKAGES_READ_TOKEN to take precedence over GITHUB_TOKEN.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void gradlePropertiesTakePrecedenceOverReadPackageEnvironmentVariables() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");
        Files.writeString(gradlePropertiesFile().toPath(), """
                gpr.user=prop-user
                gpr.key=prop-token
                """);

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GH_PACKAGES_READ_USER", "read-user",
                        "GH_PACKAGES_READ_TOKEN", "read-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: prop-user"),
                "Expected gpr.user to take precedence over GH_PACKAGES_READ_USER.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: prop-token"),
                "Expected gpr.key to take precedence over GH_PACKAGES_READ_TOKEN.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void settingsPluginUsesReadPackageEnvironmentVariablesWhenPrimaryCredentialsAreNotSet() throws IOException {
        Files.writeString(settingsFile().toPath(), """
                plugins {
                  id 'io.github.duckasteroid.github-packages-settings'
                }

                githubPackages {
                  owner = "duckAsteroid"
                  repository = "testing"
                }

                gradle.settingsEvaluated { evaluatedSettings ->
                    evaluatedSettings.pluginManagement.repositories.each { repo ->
                        println 'PM_REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'PM_REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                    evaluatedSettings.dependencyResolutionManagement.repositories.each { repo ->
                        println 'DRM_REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'DRM_REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        Files.writeString(buildFile().toPath(), """
                tasks.register('verifySettingsReadFallback') {
                    doLast {
                        println 'SETTINGS_READ_FALLBACK_OK'
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("verifySettingsReadFallback", "--stacktrace")
                .withEnvironment(Map.of(
                        "GH_PACKAGES_READ_USER", "read-user",
                        "GH_PACKAGES_READ_TOKEN", "read-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("PM_REPO_USER: read-user"),
                "Expected pluginManagement username from GH_PACKAGES_READ_USER.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("PM_REPO_TOKEN: read-token"),
                "Expected pluginManagement token from GH_PACKAGES_READ_TOKEN.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("DRM_REPO_USER: read-user"),
                "Expected dependencyResolutionManagement username from GH_PACKAGES_READ_USER.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("DRM_REPO_TOKEN: read-token"),
                "Expected dependencyResolutionManagement token from GH_PACKAGES_READ_TOKEN.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifySettingsReadFallback").getOutcome());
    }

    @Test
    void dslProfileQualifiedGradlePropertyTakesPrecedenceOverUnqualifiedAndEnvVars() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");
        Files.writeString(gradlePropertiesFile().toPath(), """
                gpr.user=global-user
                gpr.key=global-token
                gpr.personal.user=profile-user
                gpr.personal.key=profile-token
                """);

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                repositories {
                    gitHubPackages {
                        owner   = 'test-owner'
                        repo    = 'test-repo'
                        profile = 'personal'
                    }
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GITHUB_ACTOR", "env-user",
                        "GITHUB_TOKEN", "env-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: profile-user"),
                "Expected gpr.personal.user to take precedence over gpr.user and env vars.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: profile-token"),
                "Expected gpr.personal.key to take precedence over gpr.key and env vars.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void dslProfileFallsThroughToReadPackagesEnvVarsWhenProfilePropertiesMissing() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");
        Files.writeString(gradlePropertiesFile().toPath(), """
                gpr.user=global-user
                gpr.key=global-token
                """);

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                repositories {
                    gitHubPackages {
                        owner   = 'test-owner'
                        repo    = 'test-repo'
                        profile = 'missing-profile'
                    }
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GH_PACKAGES_READ_USER", "read-user",
                        "GH_PACKAGES_READ_TOKEN", "read-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: read-user"),
                "Expected fallthrough to GH_PACKAGES_READ_USER, not the unqualified gpr.user, "
                        + "when gpr.<profile>.user is missing.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: read-token"),
                "Expected fallthrough to GH_PACKAGES_READ_TOKEN, not the unqualified gpr.key, "
                        + "when gpr.<profile>.key is missing.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void dslProfileFallsThroughToGithubActorWhenProfileAndReadEnvVarsMissing() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");
        Files.writeString(gradlePropertiesFile().toPath(), """
                gpr.user=global-user
                gpr.key=global-token
                """);

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                repositories {
                    gitHubPackages {
                        owner   = 'test-owner'
                        repo    = 'test-repo'
                        profile = 'missing-profile'
                    }
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GITHUB_ACTOR", "env-user",
                        "GITHUB_TOKEN", "env-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: env-user"),
                "Expected fallthrough to GITHUB_ACTOR, not the unqualified gpr.user, "
                        + "when gpr.<profile>.user is missing.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: env-token"),
                "Expected fallthrough to GITHUB_TOKEN, not the unqualified gpr.key, "
                        + "when gpr.<profile>.key is missing.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void dslExplicitCredentialsOverrideProfileResolution() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");
        Files.writeString(gradlePropertiesFile().toPath(), """
                gpr.personal.user=profile-user
                gpr.personal.key=profile-token
                """);

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                repositories {
                    gitHubPackages {
                        owner    = 'test-owner'
                        repo     = 'test-repo'
                        profile  = 'personal'
                        username = 'explicit-user'
                        token    = 'explicit-token'
                    }
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: explicit-user"),
                "Expected explicitly set username to override profile-based resolution.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: explicit-token"),
                "Expected explicitly set token to override profile-based resolution.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void extensionProfileQualifiedGradlePropertyTakesPrecedenceOverUnqualifiedAndEnvVars() throws IOException {
        Files.writeString(settingsFile().toPath(), "rootProject.name = 'test-project'\n");
        Files.writeString(gradlePropertiesFile().toPath(), """
                gpr.user=global-user
                gpr.key=global-token
                gpr.personal.user=profile-user
                gpr.personal.key=profile-token
                """);

        Files.writeString(buildFile().toPath(), """
                plugins {
                    id 'io.github.duckasteroid.github-packages'
                }

                githubPackages {
                    owner      = 'test-owner'
                    repository = 'test-repo'
                    profile    = 'personal'
                }

                tasks.register('printRepoCredentials') {
                    doLast {
                        def repo = repositories.findByName('GitHubPackages-test-repo')
                        println 'REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("printRepoCredentials", "--stacktrace")
                .withEnvironment(Map.of(
                        "GITHUB_ACTOR", "env-user",
                        "GITHUB_TOKEN", "env-token"
                ))
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("REPO_USER: profile-user"),
                "Expected gpr.personal.user to take precedence for the githubPackages extension.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("REPO_TOKEN: profile-token"),
                "Expected gpr.personal.key to take precedence for the githubPackages extension.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":printRepoCredentials").getOutcome());
    }

    @Test
    void settingsPluginProfileQualifiedGradlePropertyTakesPrecedence() throws IOException {
        Files.writeString(settingsFile().toPath(), """
                plugins {
                  id 'io.github.duckasteroid.github-packages-settings'
                }

                githubPackages {
                  owner      = "duckAsteroid"
                  repository = "testing"
                  profile    = "personal"
                }

                gradle.settingsEvaluated { evaluatedSettings ->
                    evaluatedSettings.pluginManagement.repositories.each { repo ->
                        println 'PM_REPO_USER: ' + (repo.credentials.username ?: '')
                        println 'PM_REPO_TOKEN: ' + (repo.credentials.password ?: '')
                    }
                }
                """);
        Files.writeString(gradlePropertiesFile().toPath(), """
                gpr.user=global-user
                gpr.key=global-token
                gpr.personal.user=profile-user
                gpr.personal.key=profile-token
                """);

        Files.writeString(buildFile().toPath(), """
                tasks.register('verifySettingsProfile') {
                    doLast {
                        println 'SETTINGS_PROFILE_OK'
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("verifySettingsProfile", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.getOutput().contains("PM_REPO_USER: profile-user"),
                "Expected gpr.personal.user to take precedence in pluginManagement.repositories.\n" + result.getOutput());
        assertTrue(result.getOutput().contains("PM_REPO_TOKEN: profile-token"),
                "Expected gpr.personal.key to take precedence in pluginManagement.repositories.\n" + result.getOutput());
        assertEquals(TaskOutcome.SUCCESS, result.task(":verifySettingsProfile").getOutcome());
    }
}
