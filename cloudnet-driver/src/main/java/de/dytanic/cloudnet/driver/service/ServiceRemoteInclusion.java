package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;

public final class ServiceRemoteInclusion extends BasicJsonDocPropertyable {

    private final String url;

    private final String destination;

    public ServiceRemoteInclusion(String url, String destination) {
        this.url = url;
        this.destination = destination;
    }

    public String getUrl() {
        return this.url;
    }

    public String getDestination() {
        return this.destination;
    }
}