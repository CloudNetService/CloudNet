package de.dytanic.cloudnet.launcher.version.update.jenkins;


import com.google.gson.JsonObject;

import java.util.List;

public class JenkinsBuild {

    private final List<BuildArtifact> artifacts;

    private final List<JsonObject> actions;

    public JenkinsBuild(List<BuildArtifact> artifacts, List<JsonObject> actions) {
        this.artifacts = artifacts;
        this.actions = actions;
    }

    public List<BuildArtifact> getArtifacts() {
        return artifacts;
    }

    public String getGitCommitHash() {
        return this.actions.stream()
                .filter(jsonObject -> jsonObject.has("lastBuiltRevision"))
                .map(jsonObject -> jsonObject.getAsJsonObject("lastBuiltRevision").get("SHA1").getAsString())
                .findFirst()
                .orElse(null);
    }

    public static class BuildArtifact {

        private final String fileName;

        private final String relativePath;

        public BuildArtifact(String fileName, String relativePath) {
            this.fileName = fileName;
            this.relativePath = relativePath;
        }

        public String getFileName() {
            return fileName;
        }

        public String getRelativePath() {
            return relativePath;
        }

    }

}
