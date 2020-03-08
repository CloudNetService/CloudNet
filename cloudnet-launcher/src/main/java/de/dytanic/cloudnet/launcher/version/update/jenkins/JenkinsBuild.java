package de.dytanic.cloudnet.launcher.version.update.jenkins;


import java.util.List;

public class JenkinsBuild {

    private List<BuildArtifact> artifacts;

    private List<ChangeSetList> changeSets;

    public JenkinsBuild(List<BuildArtifact> artifacts, List<ChangeSetList> changeSets) {
        this.artifacts = artifacts;
        this.changeSets = changeSets;
    }

    public List<BuildArtifact> getArtifacts() {
        return artifacts;
    }

    public String getGitCommitHash() {
        return this.changeSets.get(0).items.get(0).commitId;
    }

    public static class BuildArtifact {

        private String fileName;

        private String relativePath;

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

    private static class ChangeSetList {

        private List<ChangeSetListItem> items;

        public ChangeSetList(List<ChangeSetListItem> items) {
            this.items = items;
        }

        private static class ChangeSetListItem {

            private String commitId;

            public ChangeSetListItem(String commitId) {
                this.commitId = commitId;
            }

        }

    }

}
