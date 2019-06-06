package de.dytanic.cloudnet.driver.module;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ModuleDependency {

    private String repo, url, group, name, version;

    public ModuleDependency(String repo, String url, String group, String name, String version) {
        this.repo = repo;
        this.url = url;
        this.group = group;
        this.name = name;
        this.version = version;
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

}