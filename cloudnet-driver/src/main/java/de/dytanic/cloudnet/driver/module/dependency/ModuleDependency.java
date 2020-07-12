package de.dytanic.cloudnet.driver.module.dependency;

import de.dytanic.cloudnet.driver.module.ModuleId;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString
@EqualsAndHashCode
public class ModuleDependency implements SerializableObject {

    private String repo, url, group, name, version;

    public ModuleDependency(String repo, String url, String group, String name, String version) {
        this.repo = repo;
        this.url = url;
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public ModuleDependency(String repo, String group, String name, String version) {
        this.repo = repo;
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public ModuleDependency(String url) {
        this.url = url;
    }

    public ModuleDependency() {
    }

    public String getRepo() {
        return this.repo;
    }

    public String getUrl() {
        return this.url;
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

    public ModuleId asModuleId() {
        return new ModuleId(this.group, this.name, this.version);
    }

    public ModuleDependencyType getType() {
        return this.url != null ? ModuleDependencyType.URL : this.repo != null ? ModuleDependencyType.MAVEN : ModuleDependencyType.MODULE;
    }

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeOptionalString(this.repo);
        buffer.writeOptionalString(this.url);
        buffer.writeOptionalString(this.group);
        buffer.writeOptionalString(this.name);
        buffer.writeOptionalString(this.version);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.repo = buffer.readOptionalString();
        this.url = buffer.readOptionalString();
        this.group = buffer.readOptionalString();
        this.name = buffer.readOptionalString();
        this.version = buffer.readOptionalString();
    }
}