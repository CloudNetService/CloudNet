package de.dytanic.cloudnet.launcher.update;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class DefaultRepositoryUpdater implements Updater {

    private String url;

    private String repositoryVersion;

    private String appVersion;

    private String gitHubRepository;

    private GitCommit latestGitCommit;

    @Override
    public boolean init(String url, String githubRepository) {
        this.url = url = url.endsWith("/") ? url : url + "/";
        this.gitHubRepository = githubRepository;

        try {

            try (InputStream inputStream = this.readFromURL(url + "repository")) {
                Properties properties = new Properties();
                properties.load(inputStream);

                this.repositoryVersion = properties.getProperty("repository-version");
                this.appVersion = properties.getProperty("app-version");

                this.latestGitCommit = this.requestLatestGitCommit(properties.getProperty("git-commit", "unknown"));
            }

            return true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public String getRepositoryVersion() {
        return this.repositoryVersion;
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
    public boolean installModuleFile(String name, Path path) {
        System.out.println("Installing remote version module " + name + " in version " + this.appVersion);

        return this.installFile(name, path, true);
    }

    @Override
    public boolean installFile(String name, Path path, boolean replace) {
        if (!replace && Files.exists(path)) {
            return true;
        }

        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);

            try (InputStream inputStream = this.readFromURL(this.url + "versions/" + this.appVersion + "/" + name)) {
                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;

        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

}