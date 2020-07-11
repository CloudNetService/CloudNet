package de.dytanic.cloudnet.driver.module;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ModuleId {

    private final String group;
    private final String name;
    private final String version;

    public ModuleId(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version != null ? version : "latest";
    }

    public ModuleId(String group, String name) {
        this(group, name, null);
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean equalsIgnoreVersion(ModuleId moduleId) {
        return this.group.equals(moduleId.group) && this.name.equals(moduleId.name);
    }

    @Override
    public String toString() {
        return this.version != null ?
                this.group + ":" + this.name + ":" + this.version :
                this.group + ":" + this.name;
    }
}
