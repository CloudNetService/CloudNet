package de.dytanic.cloudnet.driver.network.cluster;

import de.dytanic.cloudnet.common.document.gson.BasicJsonDocPropertyable;
import de.dytanic.cloudnet.driver.network.HostAndPort;

public class NetworkClusterNode extends BasicJsonDocPropertyable {

    private final String uniqueId;

    private final HostAndPort[] listeners;

    public NetworkClusterNode(String uniqueId, HostAndPort[] listeners) {
        this.uniqueId = uniqueId;
        this.listeners = listeners;
    }

    public String getUniqueId() {
        return this.uniqueId;
    }

    public HostAndPort[] getListeners() {
        return this.listeners;
    }
}