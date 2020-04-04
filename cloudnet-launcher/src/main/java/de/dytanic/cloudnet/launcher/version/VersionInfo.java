package de.dytanic.cloudnet.launcher.version;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.dytanic.cloudnet.launcher.LauncherUtils;
import de.dytanic.cloudnet.launcher.version.util.GitCommit;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;

public interface VersionInfo {

    String GITHUB_COMMIT_API_URL = "https://api.github.com/repos/%s/commits/%s";

    String getRepositoryVersion();

    String getCurrentVersion();

    String getGitHubRepository();

    GitCommit getLatestGitCommit();

    Path getTargetDirectory();

    default String getFullVersion() {
        return this.getCurrentVersion() + "-" + this.getLatestGitCommit().getShortenedSha();
    }

    default GitCommit requestLatestGitCommit(String gitCommitHash) {
        if (gitCommitHash == null) {
            return GitCommit.UNKNOWN;
        }

        String commitURL = String.format(GITHUB_COMMIT_API_URL, this.getGitHubRepository(), gitCommitHash);

        try (Reader reader = new InputStreamReader(LauncherUtils.readFromURL(commitURL))) {
            JsonElement jsonElement = JsonParser.parseReader(reader);

            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = (JsonObject) jsonElement;

                if (jsonObject.has("commit")) {

                    JsonObject commitObject = jsonObject.getAsJsonObject("commit");
                    commitObject.add("sha", jsonObject.get("sha"));

                    return LauncherUtils.GSON.fromJson(commitObject, GitCommit.class);

                }
            }
        } catch (IOException ignored) {
        }

        return new GitCommit(gitCommitHash, null, null);
    }

}
