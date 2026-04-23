package io.github.duckasteroid.gradle.githubpackages;

import groovy.lang.Closure;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.initialization.resolve.DependencyResolutionManagement;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.plugin.management.PluginManagementSpec;

/**
 * Gradle settings plugin that configures an authenticated GitHub Packages Maven repository
 * inside {@code pluginManagement} and {@code dependencyResolutionManagement}.
 *
 * <p>Apply it in {@code settings.gradle}:
 * <pre>{@code
 * plugins {
 *     id 'io.github.duckasteroid.github-packages-settings'
 * }
 *
 * githubPackages {
 *     owner      = 'my-org'
 *     repository = 'my-repo'
 * }
 * }</pre>
 *
 * <p><strong>Credential Resolution:</strong>
 * Credentials are resolved using a three-tier fallback chain (first available wins):
 * <ol>
 *   <li>Gradle properties: {@code gpr.user} / {@code gpr.key} (gradle.properties)</li>
 *   <li>Environment variables: {@code GITHUB_ACTOR} / {@code GITHUB_TOKEN}</li>
 *   <li>Environment variables: {@code GH_PACKAGES_READ_USER} / {@code GH_PACKAGES_READ_TOKEN}</li>
 * </ol>
 */
public class GithubPackagesSettingsPlugin implements Plugin<Settings> {

    @Override
    public void apply(Settings settings) {
        GithubPackagesExtension extension = settings.getExtensions()
                .create(GithubPackagesExtension.NAME, GithubPackagesExtension.class);

        GithubPackagesRepositoryDsl pluginManagementDsl =
                new GithubPackagesRepositoryDsl(settings.getPluginManagement().getRepositories(), settings.getProviders());
        ExtraPropertiesExtension extra = settings.getExtensions().getExtraProperties();
        extra.set("gitHubPackages", new Closure<Object>(settings) {
            @SuppressWarnings("unused")
            public Object doCall(Closure<?> closure) {
                pluginManagementDsl.call(closure);
                return null;
            }
        });

        settings.getGradle().settingsEvaluated(evaluatedSettings -> {
            if (!extension.getOwner().isPresent() || !extension.getRepository().isPresent()) {
                return;
            }

            String url = extension.mavenUrl();
            String username = extension.getUsername().getOrElse("");
            String token = extension.getToken().getOrElse("");
            String repoName = "GitHubPackages-" + extension.getRepository().get();

            PluginManagementSpec pluginManagement = evaluatedSettings.getPluginManagement();
            pluginManagement.getRepositories().maven(repo -> {
                repo.setName(repoName);
                repo.setUrl(url);
                repo.credentials(creds -> {
                    creds.setUsername(username);
                    creds.setPassword(token);
                });
            });

            DependencyResolutionManagement drm =
                    evaluatedSettings.getDependencyResolutionManagement();

            drm.getRepositories().maven(repo -> {
                repo.setName(repoName);
                repo.setUrl(url);
                repo.credentials(creds -> {
                    creds.setUsername(username);
                    creds.setPassword(token);
                });
            });
        });
    }
}
