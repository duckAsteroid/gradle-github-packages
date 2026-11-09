package io.github.duckasteroid.gradle.githubpackages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CredentialProviders}.
 */
class CredentialProvidersTest {

    @Test
    void missingTokenMessageNamesEverySourceAndHowToFixIt() {
        String message = CredentialProviders.TOKEN.missingCredentialMessage();

        assertTrue(message.contains(CredentialKeys.GRADLE_PROPS_KEY), "names the gradle property");
        assertTrue(message.contains(CredentialKeys.ENV_GH_READ_PACKAGES_TOKEN), "names the shared env var");
        assertTrue(message.contains(CredentialKeys.ENV_GH_TOKEN), "names the GitHub Actions env var");
        assertTrue(message.contains("gh auth token"), "names the GitHub CLI command");
        assertTrue(message.contains("gh auth refresh -s read:packages"), "tells the user how to fix it");
    }

    @Test
    void missingUsernameMessageNamesEverySource() {
        String message = CredentialProviders.USER.missingCredentialMessage();

        assertTrue(message.contains(CredentialKeys.GRADLE_PROPS_USER), "names the gradle property");
        assertTrue(message.contains(CredentialKeys.ENV_GH_READ_PACKAGES_USER), "names the shared env var");
        assertTrue(message.contains(CredentialKeys.ENV_GH_ACTOR), "names the GitHub Actions env var");
        assertTrue(message.contains(CredentialKeys.GH_CLI_USERNAME), "names the GitHub CLI username");
    }

    @Test
    void searchPathIsOrderedHighestPriorityFirst() {
        assertTrue(CredentialProviders.TOKEN.searchPath().get(0).contains(CredentialKeys.GRADLE_PROPS_KEY));
        assertTrue(CredentialProviders.TOKEN.searchPath().get(3).contains("gh auth token"));
    }
}
