package io.github.duckasteroid.gradle.githubpackages;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides standardised routes to get the authentication credentials for GitHub Packages.
 *
 * <p>Uses a four-tier fallback chain:
 * <ol>
 *   <li><strong>Tier 1 (Gradle Properties):</strong> gpr.user / gpr.key from gradle.properties -
 *       or, when a named {@code profile} is given, gpr.&lt;profile&gt;.user / gpr.&lt;profile&gt;.key
 *       instead (deliberately <em>not</em> falling back to the unqualified gpr.user/gpr.key, so a
 *       mistyped profile name can't silently pick up the wrong identity)</li>
 *   <li><strong>Tier 2 (Shared credential):</strong> GH_PACKAGES_READ_USER / GH_PACKAGES_READ_TOKEN
 *       environment variables by default, or a differently-named pair of environment variables when
 *       {@code userEnvVar} / {@code tokenEnvVar} is given</li>
 *   <li><strong>Tier 3 (GitHub Actions):</strong> GITHUB_ACTOR / GITHUB_TOKEN environment variables</li>
 *   <li><strong>Tier 4 (GitHub CLI):</strong> {@code gh auth token},
 *       so a developer already signed in with {@code gh auth login} needs no token stored on disk.
 *       Deliberately last, so a build on CI never shells out; skipped entirely when the CLI is not
 *       installed or not signed in</li>
 * </ol>
 *
 * <p>The first available source in this chain is used. Once a value is found at any tier,
 * lower tiers are not consulted.
 */
public enum CredentialProviders {
    /**
     * Resolves the GitHub username for authentication. The search path is:
     * <ol>
     *     <li>Gradle property: {@code gpr.user}, or {@code gpr.<profile>.user} when a profile is given</li>
     *     <li>Environment variable: {@code GH_PACKAGES_READ_USER}, or a custom name when {@code userEnvVar} is given</li>
     *     <li>Environment variable: {@code GITHUB_ACTOR}</li>
     *     <li>GitHub CLI: a fixed username paired with {@code gh auth token}</li>
     * </ol>
     */
    USER {
        @Override
        Provider<String> gradleProperty(ProviderFactory providers, String profile) {
            return providers.gradleProperty(
                    profile == null ? CredentialKeys.GRADLE_PROPS_USER : CredentialKeys.gradlePropsUser(profile));
        }

        @Override
        String defaultEnvVarName() {
            return CredentialKeys.ENV_GH_READ_PACKAGES_USER;
        }

        @Override
        Provider<String> githubActionsEnvVar(ProviderFactory providers) {
            return providers.environmentVariable(CredentialKeys.ENV_GH_ACTOR);
        }

        @Override
        Provider<String> gitHubCli(ProviderFactory providers) {
            // Derived from the token lookup rather than `gh api user`, so this shares that call's
            // cache entry and costs nothing: maven.pkg.github.com ignores the username anyway.
            return GitHubCli.output(providers, CredentialKeys.GH_CLI_TOKEN_ARGS)
                    .map(token -> CredentialKeys.GH_CLI_USERNAME);
        }

        @Override
        String displayName() {
            return "username";
        }

        @Override
        List<String> searchPath() {
            return List.of(
                    "Gradle property '" + CredentialKeys.GRADLE_PROPS_USER + "' (or 'gpr.<profile>.user')",
                    "environment variable '" + CredentialKeys.ENV_GH_READ_PACKAGES_USER + "' (or the name set via userEnvVar)",
                    "environment variable '" + CredentialKeys.ENV_GH_ACTOR + "'",
                    "GitHub CLI: '" + CredentialKeys.GH_CLI_USERNAME + "', when gh auth token yields a token");
        }
    },
    /**
     * Resolves the GitHub token for authentication. The search path is:
     * <ol>
     *     <li>Gradle property: {@code gpr.key}, or {@code gpr.<profile>.key} when a profile is given</li>
     *     <li>Environment variable: {@code GH_PACKAGES_READ_TOKEN}, or a custom name when {@code tokenEnvVar} is given</li>
     *     <li>Environment variable: {@code GITHUB_TOKEN}</li>
     *     <li>GitHub CLI: {@code gh auth token}</li>
     * </ol>
     */
    TOKEN {
        @Override
        Provider<String> gradleProperty(ProviderFactory providers, String profile) {
            return providers.gradleProperty(
                    profile == null ? CredentialKeys.GRADLE_PROPS_KEY : CredentialKeys.gradlePropsKey(profile));
        }

        @Override
        String defaultEnvVarName() {
            return CredentialKeys.ENV_GH_READ_PACKAGES_TOKEN;
        }

        @Override
        Provider<String> githubActionsEnvVar(ProviderFactory providers) {
            return providers.environmentVariable(CredentialKeys.ENV_GH_TOKEN);
        }

        @Override
        Provider<String> gitHubCli(ProviderFactory providers) {
            return GitHubCli.output(providers, CredentialKeys.GH_CLI_TOKEN_ARGS);
        }

        @Override
        String displayName() {
            return "token";
        }

        @Override
        List<String> searchPath() {
            return List.of(
                    "Gradle property '" + CredentialKeys.GRADLE_PROPS_KEY + "' (or 'gpr.<profile>.key')",
                    "environment variable '" + CredentialKeys.ENV_GH_READ_PACKAGES_TOKEN + "' (or the name set via tokenEnvVar)",
                    "environment variable '" + CredentialKeys.ENV_GH_TOKEN + "'",
                    "GitHub CLI: gh " + String.join(" ", CredentialKeys.GH_CLI_TOKEN_ARGS));
        }
    };

    abstract Provider<String> gradleProperty(ProviderFactory providers, String profile);

    /** Name of the tier 2 environment variable used when no override is given. */
    abstract String defaultEnvVarName();

    abstract Provider<String> githubActionsEnvVar(ProviderFactory providers);

    /** Reads the credential from a locally signed-in GitHub CLI. */
    abstract Provider<String> gitHubCli(ProviderFactory providers);

    /** What this credential is called in diagnostics, e.g. "token". */
    abstract String displayName();

    /** The sources consulted, in order, described for diagnostics. */
    abstract List<String> searchPath();

    /** Resolves credentials using no named profile - i.e. today's default gpr.user/gpr.key chain. */
    public Provider<String> apply(ProviderFactory providers) {
        return apply(providers, providers.provider(() -> null));
    }

    /**
     * Resolves credentials, preferring a named {@code profile}'s gradle properties over the
     * unqualified ones when {@code profile} has a value. An absent or blank profile behaves the
     * same as {@link #apply(ProviderFactory)}.
     */
    public Provider<String> apply(ProviderFactory providers, Provider<String> profile) {
        return apply(providers, profile, providers.provider(() -> null));
    }

    /**
     * Resolves credentials, additionally letting the tier 2 environment variable name be
     * overridden via {@code envVarName} (e.g. to read {@code MY_ORG_TOKEN} instead of the default
     * {@code GH_PACKAGES_READ_TOKEN}). An absent or blank {@code envVarName} behaves the same as
     * {@link #apply(ProviderFactory, Provider)}.
     */
    public Provider<String> apply(ProviderFactory providers, Provider<String> profile, Provider<String> envVarName) {
        Provider<String> tier1 = profile.orElse("")
                .flatMap(p -> gradleProperty(providers, p.isBlank() ? null : p));
        Provider<String> tier2 = envVarName.orElse("")
                .flatMap(name -> providers.environmentVariable(name.isBlank() ? defaultEnvVarName() : name));
        return tier1
                .orElse(tier2)
                .orElse(githubActionsEnvVar(providers))
                .orElse(gitHubCli(providers));
    }

    /**
     * As {@link #apply(ProviderFactory, Provider, Provider)}, but fails with an explanation of
     * every source that was consulted rather than resolving to no value. Without this, an
     * unresolved credential reaches GitHub as an empty string and surfaces much later as an
     * opaque {@code 401 Unauthorized} against maven.pkg.github.com.
     */
    public Provider<String> required(ProviderFactory providers, Provider<String> profile, Provider<String> envVarName) {
        return apply(providers, profile, envVarName).orElse(providers.provider(() -> {
            throw new InvalidUserDataException(missingCredentialMessage());
        }));
    }

    /** Explains where the credential was looked for and how to supply one. */
    String missingCredentialMessage() {
        return "No GitHub Packages " + displayName() + " found. Looked for:\n"
                + searchPath().stream().map(source -> "  - " + source).collect(Collectors.joining("\n"))
                + "\n\nTo authenticate with the GitHub CLI (no token stored on disk):\n"
                + "  gh auth login\n"
                + "  gh auth refresh -s read:packages\n"
                + "\nOr set '" + CredentialKeys.GRADLE_PROPS_USER + "' and '" + CredentialKeys.GRADLE_PROPS_KEY
                + "' in ~/.gradle/gradle.properties.";
    }
}
