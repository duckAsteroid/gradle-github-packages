package io.github.duckasteroid.gradle.githubpackages;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads credentials from the GitHub CLI, so that a developer who has already signed in with
 * {@code gh auth login} needs no token stored on disk.
 *
 * <p>The executable is located on {@code PATH} before anything is run, so a machine without the
 * CLI installed resolves to no value rather than failing the build. A CLI that is present but not
 * signed in exits non-zero with no output, which likewise resolves to no value.
 *
 * <p>Results are cached for {@link #CACHE_TTL_MILLIS}. Credentials are resolved once per project,
 * so without this a 40-project build runs the CLI 40 times - around 2 seconds of subprocess
 * overhead on every single Gradle invocation, because Gradle re-runs value sources each build to
 * check whether a cached configuration is still valid. The first call of each build still goes
 * through {@link ProviderFactory#exec}, so the configuration cache continues to see the CLI as an
 * input and invalidates correctly when the credential changes.
 */
final class GitHubCli {

    /**
     * How long a CLI result is reused. Long enough to cover a single build, short enough that
     * {@code gh auth logout} or {@code gh auth refresh} takes effect without restarting the daemon.
     */
    static final long CACHE_TTL_MILLIS = 60_000L;

    private static final Map<List<String>, Result> CACHE = new ConcurrentHashMap<>();

    private GitHubCli() {
    }

    /**
     * Runs {@code gh} with the given arguments and returns its trimmed standard output, or no
     * value when the CLI is absent, not signed in, or produced nothing.
     */
    static Provider<String> output(ProviderFactory providers, List<String> arguments) {
        return providers.environmentVariable(CredentialKeys.ENV_PATH)
                .map(GitHubCli::findOnPath)
                .flatMap(executable -> providers.provider(() -> cachedOutput(providers, executable, arguments)));
    }

    /** Returns the cached output if it is still live, otherwise runs the CLI and caches the result. */
    private static String cachedOutput(ProviderFactory providers, String executable, List<String> arguments) {
        Result cached = CACHE.get(arguments);
        if (cached != null && cached.isLive()) {
            return cached.value();
        }
        String value = providers.exec(spec -> {
            spec.setExecutable(executable);
            spec.setArgs(arguments);
            spec.setIgnoreExitValue(true);
        }).getStandardOutput().getAsText().map(GitHubCli::trimToNull).getOrNull();
        CACHE.put(arguments, new Result(value, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        return value;
    }

    /** The first GitHub CLI executable on the given {@code PATH}, or {@code null} if there is none. */
    static String findOnPath(String path) {
        for (String entry : path.split(File.pathSeparator)) {
            if (entry.isBlank()) {
                continue;
            }
            for (String name : CredentialKeys.GH_CLI_EXECUTABLES) {
                File candidate = new File(entry, name);
                if (candidate.isFile() && candidate.canExecute()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /** Returns {@code null} rather than an empty string, so that a provider resolves to no value. */
    static String trimToNull(String value) {
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** A CLI result, including a "not signed in" null, and the point at which it should be re-read. */
    private record Result(String value, long expiresAt) {
        boolean isLive() {
            return System.currentTimeMillis() < expiresAt;
        }
    }
}
