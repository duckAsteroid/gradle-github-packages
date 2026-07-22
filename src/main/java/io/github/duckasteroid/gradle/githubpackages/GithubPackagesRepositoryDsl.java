package io.github.duckasteroid.gradle.githubpackages;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.provider.ProviderFactory;

/**
 * DSL entrypoint for use inside a repositories block within build scripts.
 *
 * <pre>{@code
 * repositories {
 *     gitHubPackages {
 *         owner = "duckAsteroid"
 *         repo = "testing"
 *         // Optional: override credentials
 *         // username = "my-user"
 *         // token = "ghp_xxx"
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Credential Resolution:</strong>
 * If username/token are not explicitly provided, they are resolved using a three-tier fallback:
 * <ol>
 *   <li>Gradle properties: {@code gpr.user} / {@code gpr.key}</li>
 *   <li>Environment variables: {@code GH_PACKAGES_READ_USER} / {@code GH_PACKAGES_READ_TOKEN}</li>
 *   <li>Environment variables: {@code GITHUB_ACTOR} / {@code GITHUB_TOKEN}</li>
 * </ol>
 *
 * <p><strong>Target repository handler:</strong> when invoked as {@code gitHubPackages { ... }}
 * from within a {@code repositories { }} block, the repository is added to whichever
 * {@link RepositoryHandler} actually delegates that block (e.g. {@code project.repositories},
 * {@code publishing.repositories}, or {@code pluginManagement.repositories}), resolved from the
 * closure's lexical owner at call time. The handler passed to the constructor is only used as a
 * fallback when that can't be determined (e.g. the {@link Action}-based entry point, which has no
 * enclosing closure to inspect).
 */
public class GithubPackagesRepositoryDsl {

    private final RepositoryHandler fallbackRepositories;
    private final ProviderFactory providers;

    public GithubPackagesRepositoryDsl(RepositoryHandler fallbackRepositories, ProviderFactory providers) {
        this.fallbackRepositories = fallbackRepositories;
        this.providers = providers;
    }

    public void call(Closure<?> closure) {
        GithubPackagesRepositorySpec spec = new GithubPackagesRepositorySpec();
        configureDefaults(spec);
        RepositoryHandler target = resolveTargetRepositories(closure);
        // Configure from Groovy DSL: repositories { gitHubPackages { ... } }
        Closure<?> cloned = (Closure<?>) closure.clone();
        cloned.setDelegate(spec);
        cloned.setResolveStrategy(Closure.DELEGATE_FIRST);
        cloned.call();
        addRepository(target, spec);
    }

    public void call(Action<? super GithubPackagesRepositorySpec> action) {
        GithubPackagesRepositorySpec spec = new GithubPackagesRepositorySpec();
        configureDefaults(spec);
        action.execute(spec);
        addRepository(fallbackRepositories, spec);
    }

    /**
     * The {@code repositories { }} block that lexically encloses {@code gitHubPackages { ... }}
     * is the closure's owner, and that block's delegate is the actual {@link RepositoryHandler}
     * being configured (project, publishing, or pluginManagement repositories) - which may differ
     * from the handler this instance was constructed with.
     */
    private RepositoryHandler resolveTargetRepositories(Closure<?> closure) {
        Object owner = closure.getOwner();
        if (owner instanceof Closure) {
            Object delegate = ((Closure<?>) owner).getDelegate();
            if (delegate instanceof RepositoryHandler) {
                return (RepositoryHandler) delegate;
            }
        }
        return fallbackRepositories;
    }

    private void configureDefaults(GithubPackagesRepositorySpec spec) {
        spec.setUsername(CredentialProviders.USER.apply(providers).getOrElse(""));
        spec.setToken(CredentialProviders.TOKEN.apply(providers).getOrElse(""));
    }

    private void addRepository(RepositoryHandler repositories, GithubPackagesRepositorySpec spec) {
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

        /** GitHub organisation or user that owns the package repository. Required. */
        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        /** Name of the GitHub repository that hosts the packages. Required. */
        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
        }

        /**
         * GitHub username used for authentication.
         * Defaults to gpr.user / GITHUB_ACTOR / GH_PACKAGES_READ_USER via credential resolution chain.
         */
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * GitHub token used for authentication.
         * Defaults to gpr.key / GITHUB_TOKEN / GH_PACKAGES_READ_TOKEN via credential resolution chain.
         */
        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}

