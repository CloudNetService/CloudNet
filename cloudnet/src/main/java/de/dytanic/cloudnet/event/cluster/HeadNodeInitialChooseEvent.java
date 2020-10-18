package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.driver.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Gets called when the initial head node was chosen or no previous head node was found.
 */
public class HeadNodeInitialChooseEvent extends Event {

    private final NodeServer nodeServer;

    public HeadNodeInitialChooseEvent(NodeServer nodeServer) {
        this.nodeServer = nodeServer;
    }

    @NotNull
    public NodeServer getNodeServer() {
        return this.nodeServer;
    }
}
