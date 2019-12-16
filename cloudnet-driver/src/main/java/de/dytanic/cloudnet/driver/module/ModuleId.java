package de.dytanic.cloudnet.driver.module;

public class ModuleId {

    private String group;
    private String name;
    private String version;

    public ModuleId(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
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
}
