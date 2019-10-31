package de.dytanic.cloudnet.template.install;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

public class ServiceVersion {

    private String name;
    private String url;
    private JsonDocument properties;

    public ServiceVersion(String name, String url, JsonDocument properties) {
        this.name = name;
        this.url = url;
        this.properties = properties;
    }

    public ServiceVersion(String name, String url) {
        this(name, url, new JsonDocument());
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public JsonDocument getProperties() {
        return properties;
    }
}
