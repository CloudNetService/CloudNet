package de.dytanic.cloudnet.driver.module;

public class ModuleId {

    private String group;
    private String name;
    private String version;

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

    @Override
    public String toString() {
        return this.version != null ?
                this.group + ":" + this.name + ":" + this.version :
                this.group + ":" + this.name;
    }
}
