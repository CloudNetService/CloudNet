package de.dytanic.cloudnet.launcher.version.update.jenkins;


import java.util.List;

public class JenkinsBuild {

    private final List<BuildArtifact> artifacts;

    private final List<ChangeSetList> changeSets;

    public JenkinsBuild(List<BuildArtifact> artifacts, List<ChangeSetList> changeSets) {
        this.artifacts = artifacts;
        this.changeSets = changeSets;
    }

    public List<BuildArtifact> getArtifacts() {
        return artifacts;
    }

    public String getGitCommitHash() {
        if (this.changeSets.size() > 0) {
            ChangeSetList changeSetList = this.changeSets.get(0);

            if (changeSetList.items.size() > 0) {
                return changeSetList.items.get(changeSetList.items.size() - 1).commitId;
            }
        }
        return null;
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

    private static class ChangeSetList {

        private final List<ChangeSetListItem> items;

        public ChangeSetList(List<ChangeSetListItem> items) {
            this.items = items;
        }

        private static class ChangeSetListItem {

            private final String commitId;

            public ChangeSetListItem(String commitId) {
                this.commitId = commitId;
            }

        }

    }

}
