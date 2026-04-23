package io.github.duckasteroid.gradle.githubpackages;

/**
 * Centralized credential key names used by this plugin.
 *
 * <p>The keys are grouped by source:
 * <ul>
 *   <li>Gradle properties used from {@code gradle.properties}</li>
 *   <li>GitHub Actions default environment variables</li>
 *   <li>Custom environment variables for read-only package access</li>
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
}

