package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

public class PluginInfo extends BasicJsonDocPropertyable {

    private final String name, version;

    public PluginInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }
}