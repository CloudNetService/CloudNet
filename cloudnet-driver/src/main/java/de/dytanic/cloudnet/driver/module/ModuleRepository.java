package de.dytanic.cloudnet.driver.module;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class ModuleRepository {

    private String name, url;

    public ModuleRepository(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public ModuleRepository() {
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

}