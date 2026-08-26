package io.github.duckasteroid.gradle.githubpackages;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

/**
 * Provides standardised routes to get the authentication credentials for GitHub Packages.
 *
 * <p>Uses a three-tier fallback chain:
 * <ol>
 *   <li><strong>Tier 1 (Gradle Properties):</strong> gpr.user / gpr.key from gradle.properties -
 *       or, when a named {@code profile} is given, gpr.&lt;profile&gt;.user / gpr.&lt;profile&gt;.key
 *       instead (deliberately <em>not</em> falling back to the unqualified gpr.user/gpr.key, so a
 *       mistyped profile name can't silently pick up the wrong identity)</li>
 *   <li><strong>Tier 2 (Shared credential):</strong> GH_PACKAGES_READ_USER / GH_PACKAGES_READ_TOKEN
 *       environment variables by default, or a differently-named pair of environment variables when
 *       {@code userEnvVar} / {@code tokenEnvVar} is given</li>
 *   <li><strong>Tier 3 (GitHub Actions):</strong> GITHUB_ACTOR / GITHUB_TOKEN environment variables</li>
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
    },
    /**
     * Resolves the GitHub token for authentication. The search path is:
     * <ol>
     *     <li>Gradle property: {@code gpr.key}, or {@code gpr.<profile>.key} when a profile is given</li>
     *     <li>Environment variable: {@code GH_PACKAGES_READ_TOKEN}, or a custom name when {@code tokenEnvVar} is given</li>
     *     <li>Environment variable: {@code GITHUB_TOKEN}</li>
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
    };

    abstract Provider<String> gradleProperty(ProviderFactory providers, String profile);

    /** Name of the tier 2 environment variable used when no override is given. */
    abstract String defaultEnvVarName();

    abstract Provider<String> githubActionsEnvVar(ProviderFactory providers);

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
                .orElse(githubActionsEnvVar(providers));
    }
}
