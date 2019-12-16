package de.dytanic.cloudnet.module.repository;

import de.dytanic.cloudnet.driver.module.ModuleId;

public class RepositoryModuleInfo {

    private ModuleId moduleId;
    private String[] authors;
    private String[] depends;
    private String website;
    private String sourceUrl;
    private String supportUrl;
    private String downloadUrl;

    public RepositoryModuleInfo(ModuleId moduleId, String[] authors, String[] depends, String website, String sourceUrl, String supportUrl, String downloadUrl) {
        this.moduleId = moduleId;
        this.authors = authors;
        this.depends = depends;
        this.website = website;
        this.sourceUrl = sourceUrl;
        this.supportUrl = supportUrl;
        this.downloadUrl = downloadUrl;
    }

    public ModuleId getModuleId() {
        return moduleId;
    }

    public String[] getAuthors() {
        return authors;
    }

    public String[] getDepends() {
        return depends;
    }

    public String getWebsite() {
        return website;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSupportUrl() {
        return supportUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
