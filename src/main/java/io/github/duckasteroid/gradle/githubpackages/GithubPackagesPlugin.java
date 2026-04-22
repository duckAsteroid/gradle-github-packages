package io.github.duckasteroid.gradle.githubpackages;

import groovy.lang.Closure;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.publish.PublishingExtension;

/**
 * Gradle project plugin that configures an authenticated GitHub Packages Maven repository.
 *
 * <p>Apply it to a project and configure the extension:
 * <pre>{@code
 * plugins {
 *     id 'io.github.duckasteroid.github-packages'
 * }
 *
 * githubPackages {
 *     owner      = 'my-org'
 *     repository = 'my-repo'
 * }
 * }</pre>
 *
 * <p>The plugin will:
 * <ul>
 *   <li>Add the GitHub Packages Maven URL to {@code repositories} (for dependency resolution).</li>
 *   <li>If the {@code maven-publish} plugin is present, add the same URL to
 *       {@code publishing.repositories}.</li>
 * </ul>
 */
public class GithubPackagesPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        GithubPackagesExtension extension = project.getExtensions()
                .create(GithubPackagesExtension.NAME, GithubPackagesExtension.class);

        GithubPackagesRepositoryDsl repositoriesDsl =
                new GithubPackagesRepositoryDsl(project.getRepositories(), project.getProviders());
        ExtraPropertiesExtension extra = project.getExtensions().getExtraProperties();
        extra.set("gitHubPackages", new Closure<Object>(project) {
            @SuppressWarnings("unused")
            public Object doCall(Closure<?> closure) {
                repositoriesDsl.call(closure);
                return null;
            }
        });

        // Register the Maven repo for dependency resolution after the project is evaluated
        // so the extension values are fully configured.
        project.afterEvaluate(p -> {
            // Legacy/global style: githubPackages { owner/repository ... }
            if (extension.getOwner().isPresent() && extension.getRepository().isPresent()) {
                String url = extension.mavenUrl();
                String repoName = "GitHubPackages-" + extension.getRepository().get();
                String username = extension.getUsername().getOrElse("");
                String token = extension.getToken().getOrElse("");

                addOrUpdateMavenRepo(p.getRepositories(), repoName, url, username, token);

                // ── Publishing repository (optional) ────────────────────────
                p.getPluginManager().withPlugin("maven-publish", appliedPlugin -> {
                    PublishingExtension publishing = p.getExtensions().getByType(PublishingExtension.class);
                    addOrUpdateMavenRepo(publishing.getRepositories(), repoName, url, username, token);
                });
            }
        });
    }

    private void addOrUpdateMavenRepo(RepositoryHandler repositories,
                                      String repoName,
                                      String url,
                                      String username,
                                      String token) {
        repositories.maven(repo -> {
            repo.setName(repoName);
            repo.setUrl(url);
            repo.credentials(creds -> {
                creds.setUsername(username);
                creds.setPassword(token);
            });
        });
    }
}
