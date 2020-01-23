package de.dytanic.cloudnet.launcher.version;


import de.dytanic.cloudnet.launcher.version.util.GitCommit;

import java.nio.file.Path;

public class InstalledVersionInfo implements VersionInfo {

    protected Path targetDirectory;

    protected String gitHubRepository;

    protected String appVersion;

    protected GitCommit latestGitCommit;

    public InstalledVersionInfo(Path targetDirectory, String gitHubRepository) {
        this.targetDirectory = targetDirectory;
        this.gitHubRepository = gitHubRepository;

        String versionSpecification = targetDirectory.getFileName().toString();

        String[] versionParts = versionSpecification.split("-");

        if (versionParts.length > 1) {
            this.appVersion = versionParts[0] + "-" + versionParts[1];

            String gitCommitHash = versionParts.length > 2 ? versionParts[2] : null;
            this.latestGitCommit = this.requestLatestGitCommit(gitCommitHash);
        }

    }

    @Override
    public String getRepositoryVersion() {
        return null;
    }

    @Override
    public String getCurrentVersion() {
        return this.appVersion;
    }

    @Override
    public String getGitHubRepository() {
        return this.gitHubRepository;
    }

    @Override
    public GitCommit getLatestGitCommit() {
        return this.latestGitCommit;
    }

    @Override
    public Path getTargetDirectory() {
        return this.targetDirectory;
    }

}
