package io.github.duckasteroid.gradle.githubpackages;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.provider.ProviderFactory;

/**
 * DSL entrypoint for use inside a repositories block:
 *
 * <pre>{@code
 * repositories {
 *     gitHubPackages {
 *         owner = "duckAsteroid"
 *         repo = "testing"
 *     }
 * }
 * }</pre>
 */
public class GithubPackagesRepositoryDsl {

    private final RepositoryHandler repositories;
    private final ProviderFactory providers;

    public GithubPackagesRepositoryDsl(RepositoryHandler repositories, ProviderFactory providers) {
        this.repositories = repositories;
        this.providers = providers;
    }

    public void call(Closure<?> closure) {
        GithubPackagesRepositorySpec spec = new GithubPackagesRepositorySpec();
        configureDefaults(spec);
        // Configure from Groovy DSL: repositories { gitHubPackages { ... } }
        Closure<?> cloned = (Closure<?>) closure.clone();
        cloned.setDelegate(spec);
        cloned.setResolveStrategy(Closure.DELEGATE_FIRST);
        cloned.call();
        addRepository(spec);
    }

    public void call(Action<? super GithubPackagesRepositorySpec> action) {
        GithubPackagesRepositorySpec spec = new GithubPackagesRepositorySpec();
        configureDefaults(spec);
        action.execute(spec);
        addRepository(spec);
    }

    private void configureDefaults(GithubPackagesRepositorySpec spec) {
        spec.setUsername(providers.gradleProperty("gpr.user")
                .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                .getOrElse(""));
        spec.setToken(providers.gradleProperty("gpr.key")
                .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                .getOrElse(""));
    }

    private void addRepository(GithubPackagesRepositorySpec spec) {
        String owner = required(spec.getOwner(), "owner");
        String repo = required(spec.getRepo(), "repo");
        String url = "https://maven.pkg.github.com/" + owner + "/" + repo;

        repositories.maven(mavenRepo -> {
            mavenRepo.setName("GitHubPackages-" + repo);
            mavenRepo.setUrl(url);
            mavenRepo.credentials(creds -> {
                creds.setUsername(spec.getUsername());
                creds.setPassword(spec.getToken());
            });
        });
    }

    private String required(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("gitHubPackages { " + key + " = '...' } is required");
        }
        return value;
    }

    public static class GithubPackagesRepositorySpec {
        private String owner;
        private String repo;
        private String username;
        private String token;

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}

