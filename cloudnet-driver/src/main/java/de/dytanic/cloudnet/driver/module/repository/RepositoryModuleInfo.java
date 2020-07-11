package de.dytanic.cloudnet.driver.module.repository;

import de.dytanic.cloudnet.driver.module.ModuleId;

public class RepositoryModuleInfo {

    private final ModuleId moduleId;
    private final String[] authors;
    private final ModuleId[] depends;
    private final ModuleId[] conflicts;
    private final String requiredCloudNetVersion;
    private final String description;
    private final String website;
    private final String sourceUrl;
    private final String supportUrl;

    public RepositoryModuleInfo(ModuleId moduleId, String[] authors, ModuleId[] depends, ModuleId[] conflicts, String requiredCloudNetVersion, String description, String website, String sourceUrl, String supportUrl) {
        this.moduleId = moduleId;
        this.authors = authors;
        this.depends = depends;
        this.conflicts = conflicts;
        this.requiredCloudNetVersion = requiredCloudNetVersion;
        this.description = description;
        this.website = website;
        this.sourceUrl = sourceUrl;
        this.supportUrl = supportUrl;
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
}
