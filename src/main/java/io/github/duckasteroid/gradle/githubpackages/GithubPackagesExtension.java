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
 *     // username and token default from gpr.user/gpr.key (gradle.properties),
 *     // then fall back to GITHUB_ACTOR / GITHUB_TOKEN
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
     * Defaults to {@code gpr.user} from gradle.properties, then {@code GITHUB_ACTOR}.
     */
    public abstract Property<String> getUsername();

    /**
     * GitHub token used for authentication.
     * Defaults to {@code gpr.key} from gradle.properties, then {@code GITHUB_TOKEN}.
     */
    public abstract Property<String> getToken();

    @Inject
    public GithubPackagesExtension(ObjectFactory objects, ProviderFactory providers) {
        // Prefer gradle.properties (project or ~/.gradle), then fall back to env vars.
        getUsername().convention(
                providers.gradleProperty("gpr.user")
                        .orElse(providers.environmentVariable("GITHUB_ACTOR"))
        );
        getToken().convention(
                providers.gradleProperty("gpr.key")
                        .orElse(providers.environmentVariable("GITHUB_TOKEN"))
        );
    }

    /** Returns the Maven URL for this GitHub Packages repository. */
    public String mavenUrl() {
        return "https://maven.pkg.github.com/"
                + getOwner().get()
                + "/"
                + getRepository().get();
    }
}
