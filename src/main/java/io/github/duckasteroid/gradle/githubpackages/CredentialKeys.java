package io.github.duckasteroid.gradle.githubpackages;

import java.util.List;

/**
 * Centralized credential key names used by this plugin.
 *
 * <p>The keys are grouped by source:
 * <ul>
 *   <li>Gradle properties used from {@code gradle.properties}</li>
 *   <li>GitHub Actions default environment variables</li>
 *   <li>Custom environment variables for read-only package access</li>
 *   <li>The GitHub CLI, for a developer already signed in locally</li>
 * </ul>
 */
public interface CredentialKeys {
    /** Gradle property key for the GitHub Packages username. */
    String GRADLE_PROPS_USER = "gpr.user";

    /** Gradle property key for the GitHub Packages token/password. */
    String GRADLE_PROPS_KEY = "gpr.key";

    /** GitHub Actions environment variable containing the actor/user name. */
    String ENV_GH_ACTOR = "GITHUB_ACTOR";

    /** GitHub Actions environment variable containing the access token. */
    String ENV_GH_TOKEN = "GITHUB_TOKEN";

    /** Custom environment variable for a read-only packages username. */
    String ENV_GH_READ_PACKAGES_USER = "GH_PACKAGES_READ_USER";

    /** Custom environment variable for a read-only packages token. */
    String ENV_GH_READ_PACKAGES_TOKEN = "GH_PACKAGES_READ_TOKEN";

    /** Environment variable searched to locate the GitHub CLI executable. */
    String ENV_PATH = "PATH";

    /** Candidate file names for the GitHub CLI executable, in lookup order. */
    List<String> GH_CLI_EXECUTABLES = List.of("gh", "gh.exe");

    /** GitHub CLI arguments that print the signed-in user's access token. */
    List<String> GH_CLI_TOKEN_ARGS = List.of("auth", "token");

    /**
     * Username paired with a GitHub CLI token. maven.pkg.github.com authenticates on the token
     * alone and ignores the username, so there is no need to spend a network round trip on
     * {@code gh api user} to discover the real login. This matches GitHub's own convention for
     * token-based Basic auth.
     */
    String GH_CLI_USERNAME = "x-access-token";

    /** Gradle property key for the username of a named credential profile, e.g. {@code gpr.personal.user}. */
    static String gradlePropsUser(String profile) {
        return "gpr." + profile + ".user";
    }

    /** Gradle property key for the token of a named credential profile, e.g. {@code gpr.personal.key}. */
    static String gradlePropsKey(String profile) {
        return "gpr." + profile + ".key";
    }
}

