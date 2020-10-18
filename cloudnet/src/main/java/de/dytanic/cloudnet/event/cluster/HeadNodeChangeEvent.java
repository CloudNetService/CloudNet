package de.dytanic.cloudnet.event.cluster;

import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.driver.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Called when the head node changed in the cluster often caused by a node disconnect.
 */
public class HeadNodeChangeEvent extends Event {

    private final NodeServer previousHeadNode;
    private final NodeServer headNode;

    public HeadNodeChangeEvent(NodeServer previousHeadNode, NodeServer headNode) {
        this.previousHeadNode = previousHeadNode;
        this.headNode = headNode;
    }

    @NotNull
    public NodeServer getPreviousHeadNode() {
        return this.previousHeadNode;
    }

    @NotNull
    public NodeServer getHeadNode() {
        return this.headNode;
    }
}
