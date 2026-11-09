package io.github.duckasteroid.gradle.githubpackages;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link GitHubCli}.
 */
class GitHubCliTest {

    @TempDir
    Path tempDir;

    @Test
    void findsExecutableOnPath() throws IOException {
        Path gh = executable("gh");

        assertEquals(gh.toFile().getAbsolutePath(), GitHubCli.findOnPath(tempDir.toString()));
    }

    @Test
    void returnsNullWhenCliIsNotInstalled() {
        assertNull(GitHubCli.findOnPath(tempDir.toString()));
    }

    @Test
    void ignoresNonExecutableFileOfTheSameName() throws IOException {
        Path gh = tempDir.resolve("gh");
        Files.createFile(gh);
        gh.toFile().setExecutable(false);

        assertNull(GitHubCli.findOnPath(tempDir.toString()));
    }

    @Test
    void searchesPathEntriesInOrderAndSkipsBlankOnes() throws IOException {
        Path second = Files.createDirectory(tempDir.resolve("second"));
        Path gh = executable(second, "gh");
        String path = String.join(File.pathSeparator, "", tempDir.toString(), second.toString());

        assertEquals(gh.toFile().getAbsolutePath(), GitHubCli.findOnPath(path));
    }

    @Test
    void trimsOutputAndTreatsEmptyAsNoValue() {
        assertEquals("ghp_token", GitHubCli.trimToNull("  ghp_token\n"));
        assertNull(GitHubCli.trimToNull("   \n "));
        assertNull(GitHubCli.trimToNull(""));
    }

    private Path executable(String name) throws IOException {
        return executable(tempDir, name);
    }

    private Path executable(Path directory, String name) throws IOException {
        Path file = Files.createFile(directory.resolve(name));
        file.toFile().setExecutable(true);
        return file;
    }
}
