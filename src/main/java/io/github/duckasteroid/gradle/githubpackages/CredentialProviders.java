package io.github.duckasteroid.gradle.githubpackages;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import java.util.function.Function;

/**
 * Provides standardised routes to get the authentication credentials for GitHub Packages.
 *
 * <p>Uses a three-tier fallback chain:
 * <ol>
 *   <li><strong>Tier 1 (Gradle Properties):</strong> gpr.user / gpr.key from gradle.properties</li>
 *   <li><strong>Tier 2 (GitHub Actions):</strong> GITHUB_ACTOR / GITHUB_TOKEN environment variables</li>
 *   <li><strong>Tier 3 (Read-Only):</strong> GH_PACKAGES_READ_USER / GH_PACKAGES_READ_TOKEN environment variables</li>
 * </ol>
 *
 * <p>The first available source in this chain is used. Once a value is found at any tier,
 * lower tiers are not consulted.
 */
public enum CredentialProviders implements Function<ProviderFactory, Provider<String>> {
    /**
     * Resolves the GitHub username for authentication. The search path is:
     * <ol>
     *     <li>Gradle property: {@code gpr.user}</li>
     *     <li>Environment variable: {@code GITHUB_ACTOR}</li>
     *     <li>Environment variable: {@code GH_PACKAGES_READ_USER}</li>
     * </ol>
     */
    USER {
        @Override
        public Provider<String> apply(ProviderFactory providers) {
            return providers.gradleProperty(CredentialKeys.GRADLE_PROPS_USER)
                    .orElse(providers.environmentVariable(CredentialKeys.ENV_GH_ACTOR))
                    .orElse(providers.environmentVariable(CredentialKeys.ENV_GH_READ_PACKAGES_USER));
        }
    },
    /**
     * Resolves the GitHub token for authentication. The search path is:
     * <ol>
     *     <li>Gradle property: {@code gpr.key}</li>
     *     <li>Environment variable: {@code GITHUB_TOKEN}</li>
     *     <li>Environment variable: {@code GH_PACKAGES_READ_TOKEN}</li>
     * </ol>
     */
    TOKEN {
        @Override
        public Provider<String> apply(ProviderFactory providers) {
            return providers.gradleProperty(CredentialKeys.GRADLE_PROPS_KEY)
                    .orElse(providers.environmentVariable(CredentialKeys.ENV_GH_TOKEN))
                    .orElse(providers.environmentVariable(CredentialKeys.ENV_GH_READ_PACKAGES_TOKEN));
        }
    }


}

