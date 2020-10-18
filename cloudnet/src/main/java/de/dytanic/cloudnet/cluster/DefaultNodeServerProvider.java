package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.event.cluster.HeadNodeChangeEvent;
import de.dytanic.cloudnet.event.cluster.HeadNodeInitialChooseEvent;
import de.dytanic.cloudnet.util.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultNodeServerProvider implements NodeServerProvider {

    private final Collection<NodeServer> nodeServers = new CopyOnWriteArrayList<>();
    private final AtomicReference<Identity<NodeServer>> headNode = new AtomicReference<>();
    private NodeServer currentNodeServer;

    @Override
    @UnmodifiableView
    public @NotNull Collection<NodeServer> getRegisteredNodeServers() {
        return Collections.unmodifiableCollection(this.nodeServers);
    }

    @Override
    public @Nullable NodeServer getNodeServer(@NotNull String uniqueId) {
        return this.nodeServers.stream()
                .filter(nodeServer -> nodeServer.getNodeInfo().getUniqueId().equals(uniqueId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public @NotNull Identity<NodeServer> getHeadNode() {
        this.syncHeadNode();
        return this.headNode.get();
    }

    @Override
    public @NotNull NodeServer getCurrentNodeServer() {
        return this.currentNodeServer;
    }

    protected void registerNode(NodeServer nodeServer) {
        if (this.getNodeServer(nodeServer.getNodeInfo().getUniqueId()) == null) {
            this.nodeServers.add(nodeServer);
            this.syncHeadNode();
        }
    }

    public void buildCurrentNodeServer() {
        this.registerNode(this.currentNodeServer = new LocalNodeServer(this, CloudNet.getInstance()));
    }

    protected void unregisterNode(NodeServer nodeServer) {
        this.unregisterNode(nodeServer.getNodeInfo().getUniqueId());
    }

    protected void unregisterNode(String uniqueId) {
        if (this.nodeServers.removeIf(
                server -> server.getNodeInfo().getUniqueId().equals(uniqueId)
        )) {
            this.ensureLocalNodeStillLoaded();
            this.syncHeadNode();
        }
    }

    private void ensureLocalNodeStillLoaded() {
        if (this.nodeServers.isEmpty() || this.getNodeServer(CloudNet.getInstance().getComponentName()) == null) {
            this.nodeServers.add(this.currentNodeServer);
        }
    }

    private void syncHeadNode() {
        NodeServer head = this.nodeServers.stream()
                .filter(server -> server.getNodeInfoSnapshot() != null)
                .min(Comparator.comparingLong(server -> server.getNodeInfoSnapshot().getStartupNanos()))
                .orElse(this.currentNodeServer);
        if (head == null) {
            return;
        }

        if (this.headNode.get() == null) {
            this.headNode.set(new NodeServerIdentity(head));
            CloudNetDriver.getInstance().getEventManager().callEvent(new HeadNodeInitialChooseEvent(head));
        } else {
            NodeServer previous = this.headNode.get().instance();
            this.headNode.set(new NodeServerIdentity(head));
            CloudNetDriver.getInstance().getEventManager().callEvent(new HeadNodeChangeEvent(previous, head));
        }
    }

    private static final class NodeServerIdentity implements Identity<NodeServer> {

        private final NodeServer nodeServer;

        private NodeServerIdentity(NodeServer nodeServer) {
            this.nodeServer = nodeServer;
        }

        @Override
        public NodeServer instance() {
            return this.nodeServer;
        }

        @Override
        public boolean isInstance(NodeServer nodeServer) {
            return this.nodeServer.equals(nodeServer);
        }
    }
}
