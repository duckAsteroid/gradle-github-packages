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
     * GitHub username used for authentication.
     * Resolved via three-tier fallback:
     * <ol>
     *   <li>{@code gpr.user} from gradle.properties (project or ~/.gradle/gradle.properties)</li>
     *   <li>{@code GITHUB_ACTOR} environment variable</li>
     *   <li>{@code GH_PACKAGES_READ_USER} environment variable</li>
     * </ol>
     */
    public abstract Property<String> getUsername();

    /**
     * GitHub token used for authentication.
     * Resolved via three-tier fallback:
     * <ol>
     *   <li>{@code gpr.key} from gradle.properties (project or ~/.gradle/gradle.properties)</li>
     *   <li>{@code GITHUB_TOKEN} environment variable</li>
     *   <li>{@code GH_PACKAGES_READ_TOKEN} environment variable</li>
     * </ol>
     */
    public abstract Property<String> getToken();

    @Inject
    public GithubPackagesExtension(ObjectFactory objects, ProviderFactory providers) {
        getUsername().convention(CredentialProviders.USER.apply(providers).getOrElse(""));
        getToken().convention(CredentialProviders.TOKEN.apply(providers).getOrElse(""));
    }

    /** Returns the Maven URL for this GitHub Packages repository. */
    public String mavenUrl() {
        return "https://maven.pkg.github.com/"
                + getOwner().get()
                + "/"
                + getRepository().get();
    }
}
