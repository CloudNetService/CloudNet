package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ModuleConfiguration {

    protected boolean runtimeModule;

    protected boolean storesSensitiveData;

    protected String
            group,
            name,
            version,
            main,
            description,
            author,
            website;

    //protected ModuleUpdateServiceConfiguration updateServiceConfiguration;

    protected ModuleRepository[] repos;

    protected ModuleDependency[] dependencies;

    protected JsonDocument properties;

    public ModuleConfiguration(boolean runtimeModule, boolean storesSensitiveData, String group, String name, String version, String main, String description, String author, String website, ModuleRepository[] repos, ModuleDependency[] dependencies, JsonDocument properties) {
        this.runtimeModule = runtimeModule;
        this.storesSensitiveData = storesSensitiveData;
        this.group = group;
        this.name = name;
        this.version = version;
        this.main = main;
        this.description = description;
        this.author = author;
        this.website = website;
        this.repos = repos;
        this.dependencies = dependencies;
        this.properties = properties;
    }

    @Deprecated
    public ModuleConfiguration(boolean runtimeModule, String group, String name, String version, String main, String description, String author, String website, ModuleRepository[] repos, ModuleDependency[] dependencies, JsonDocument properties) {
        this(runtimeModule, false, group, name, version, main, description, author, website, repos, dependencies, properties);
    }

    public ModuleConfiguration() {
    }

    public String getMainClass() {
        return this.main;
    }

    public boolean isRuntimeModule() {
        return this.runtimeModule;
    }

    public boolean storesSensitiveData() {
        return this.storesSensitiveData;
    }

    public String getGroup() {
        return this.group;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getMain() {
        return this.main;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getWebsite() {
        return this.website;
    }

    public ModuleRepository[] getRepos() {
        return this.repos;
    }

    public ModuleDependency[] getDependencies() {
        return this.dependencies;
    }

    public JsonDocument getProperties() {
        return this.properties;
    }

}