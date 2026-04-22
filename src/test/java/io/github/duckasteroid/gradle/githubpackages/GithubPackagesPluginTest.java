package io.github.duckasteroid.gradle.githubpackages;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link GithubPackagesPlugin}.
 */
class GithubPackagesPluginTest {

    @Test
    void pluginRegistersExtension() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("io.github.duckasteroid.github-packages");

        assertNotNull(
                project.getExtensions().findByName(GithubPackagesExtension.NAME),
                "Expected the 'githubPackages' extension to be registered"
        );
    }
}

