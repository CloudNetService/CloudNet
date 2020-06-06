package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class ModuleConfiguration implements SerializableObject {

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
    @ApiStatus.ScheduledForRemoval(inVersion = "3.5")
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

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeBoolean(this.runtimeModule);
        buffer.writeBoolean(this.storesSensitiveData);
        buffer.writeString(this.group);
        buffer.writeString(this.name);
        buffer.writeString(this.version);
        buffer.writeString(this.main);
        buffer.writeOptionalString(this.description);
        buffer.writeOptionalString(this.author);
        buffer.writeOptionalString(this.website);
        buffer.writeBoolean(this.repos != null);
        if (this.repos != null) {
            buffer.writeObjectArray(this.repos);
        }
        buffer.writeBoolean(this.dependencies != null);
        if (this.dependencies != null) {
            buffer.writeObjectArray(this.dependencies);
        }
        buffer.writeBoolean(this.properties != null);
        if (this.properties != null) {
            buffer.writeJsonDocument(this.properties);
        }
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.runtimeModule = buffer.readBoolean();
        this.storesSensitiveData = buffer.readBoolean();
        this.group = buffer.readString();
        this.name = buffer.readString();
        this.version = buffer.readString();
        this.main = buffer.readString();
        this.description = buffer.readOptionalString();
        this.author = buffer.readOptionalString();
        this.website = buffer.readOptionalString();
        this.repos = buffer.readBoolean() ? buffer.readObjectArray(ModuleRepository.class) : null;
        this.dependencies = buffer.readBoolean() ? buffer.readObjectArray(ModuleDependency.class) : null;
        this.properties = buffer.readBoolean() ? buffer.readJsonDocument() : null;
    }
}