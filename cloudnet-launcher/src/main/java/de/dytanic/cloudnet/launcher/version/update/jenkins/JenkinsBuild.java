package de.dytanic.cloudnet.launcher.version.update.jenkins;


import java.util.List;

public class JenkinsBuild {

    private final List<BuildArtifact> artifacts;

    private final long timestamp;

    public JenkinsBuild(List<BuildArtifact> artifacts, long timestamp) {
        this.artifacts = artifacts;
        this.timestamp = timestamp;
    }

    public List<BuildArtifact> getArtifacts() {
        return artifacts;
    }

    public long getTimestamp() {
        return timestamp;
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
