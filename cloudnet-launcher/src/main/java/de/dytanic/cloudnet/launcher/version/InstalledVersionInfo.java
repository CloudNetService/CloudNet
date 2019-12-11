package de.dytanic.cloudnet.launcher.version;


import de.dytanic.cloudnet.launcher.update.GitCommit;

public class InstalledVersionInfo implements VersionInfo {

    @Override
    public String getRepositoryVersion() {
        return null;
    }

    @Override
    public String getCurrentVersion() {
        return null;
    }

    @Override
    public String getGitHubRepository() {
        return null;
    }

    @Override
    public GitCommit getLatestGitCommit() {
        return null;
    }

}
