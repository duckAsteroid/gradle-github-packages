package io.github.duckasteroid.gradle.githubpackages;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

/**
 * Extension for configuring the GitHub Packages plugin.
 *
 * <pre>{@code
 * githubPackages {
 *     owner      = 'my-org'
 *     repository = 'my-repo'
 *     // username and token default via credential resolution chain:
 *     // 1. gpr.user / gpr.key (gradle.properties)
 *     // 2. GITHUB_ACTOR / GITHUB_TOKEN (environment)
 *     // 3. GH_PACKAGES_READ_USER / GH_PACKAGES_READ_TOKEN (environment)
 * }
 * }</pre>
 */
public abstract class GithubPackagesExtension {

    public static final String NAME = "githubPackages";

    /** GitHub organisation or user that owns the package repository. */
    public abstract Property<String> getOwner();

    /** Name of the GitHub repository that hosts the packages. */
    public abstract Property<String> getRepository();

    /**
     * Optional named credential profile. When set, credentials are resolved from
     * {@code gpr.<profile>.user} / {@code gpr.<profile>.key} instead of the unqualified
     * {@code gpr.user} / {@code gpr.key}, letting a repo use a different identity (e.g. a personal
     * GitHub account) than whatever is configured globally in {@code ~/.gradle/gradle.properties}.
     * If no matching profile-qualified properties are found, resolution still falls through to the
     * {@code GH_PACKAGES_READ_*} and {@code GITHUB_ACTOR}/{@code GITHUB_TOKEN} environment variable
     * tiers - it deliberately does not fall back to the unqualified {@code gpr.user}/{@code gpr.key}.
     */
    public abstract Property<String> getProfile();

    /**
     * GitHub username used for authentication.
     * Resolved via three-tier fallback:
     * <ol>
     *   <li>{@code gpr.user} from gradle.properties (project or ~/.gradle/gradle.properties),
     *       or {@code gpr.<profile>.user} when {@link #getProfile()} is set</li>
     *   <li>{@code GH_PACKAGES_READ_USER} environment variable</li>
     *   <li>{@code GITHUB_ACTOR} environment variable</li>
     * </ol>
     */
    public abstract Property<String> getUsername();

    /**
     * GitHub token used for authentication.
     * Resolved via three-tier fallback:
     * <ol>
     *   <li>{@code gpr.key} from gradle.properties (project or ~/.gradle/gradle.properties),
     *       or {@code gpr.<profile>.key} when {@link #getProfile()} is set</li>
     *   <li>{@code GH_PACKAGES_READ_TOKEN} environment variable</li>
     *   <li>{@code GITHUB_TOKEN} environment variable</li>
     * </ol>
     */
    public abstract Property<String> getToken();

    @Inject
    public GithubPackagesExtension(ObjectFactory objects, ProviderFactory providers) {
        getUsername().convention(CredentialProviders.USER.apply(providers, getProfile()).orElse(""));
        getToken().convention(CredentialProviders.TOKEN.apply(providers, getProfile()).orElse(""));
    }

    /** Returns the Maven URL for this GitHub Packages repository. */
    public String mavenUrl() {
        return "https://maven.pkg.github.com/"
                + getOwner().get()
                + "/"
                + getRepository().get();
    }
}
