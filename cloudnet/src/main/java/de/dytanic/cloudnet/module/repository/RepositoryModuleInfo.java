package de.dytanic.cloudnet.module.repository;

import de.dytanic.cloudnet.driver.module.ModuleId;

public class RepositoryModuleInfo {

    private ModuleId moduleId;
    private String[] authors;
    private ModuleId[] depends;
    private ModuleId[] conflicts;
    private String requiredCloudNetVersion;
    private String description;
    private String website;
    private String sourceUrl;
    private String supportUrl;
    private String downloadUrl;

    public RepositoryModuleInfo(ModuleId moduleId, String[] authors, ModuleId[] depends, ModuleId[] conflicts, String requiredCloudNetVersion, String description, String website, String sourceUrl, String supportUrl, String downloadUrl) {
        this.moduleId = moduleId;
        this.authors = authors;
        this.depends = depends;
        this.conflicts = conflicts;
        this.requiredCloudNetVersion = requiredCloudNetVersion;
        this.description = description;
        this.website = website;
        this.sourceUrl = sourceUrl;
        this.supportUrl = supportUrl;
        this.downloadUrl = downloadUrl;
    }

    public ModuleId getModuleId() {
        return moduleId;
    }

    public String getRequiredCloudNetVersion() {
        return this.requiredCloudNetVersion;
    }

    public ModuleId[] getConflicts() {
        return this.conflicts;
    }

    public String[] getAuthors() {
        return this.authors;
    }

    public ModuleId[] getDepends() {
        return this.depends;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getSourceUrl() {
        return this.sourceUrl;
    }

    public String getSupportUrl() {
        return this.supportUrl;
    }

    public String getDescription() {
        return this.description;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }
}
