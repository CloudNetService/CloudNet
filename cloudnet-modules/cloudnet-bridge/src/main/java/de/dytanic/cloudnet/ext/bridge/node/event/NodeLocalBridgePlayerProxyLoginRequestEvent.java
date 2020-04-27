package de.dytanic.cloudnet.ext.bridge.node.event;

import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.ext.bridge.player.NetworkConnectionInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeLocalBridgePlayerProxyLoginRequestEvent extends Event implements ICancelable {

    private final NetworkConnectionInfo connectionInfo;
    private String kickReason;
    private boolean cancelled;

    public NodeLocalBridgePlayerProxyLoginRequestEvent(@NotNull NetworkConnectionInfo connectionInfo, String kickReason) {
        this.connectionInfo = connectionInfo;
        this.kickReason = kickReason;
    }

    public NetworkConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

    @Nullable
    public String getKickReason() {
        return this.kickReason;
    }

    public void setKickReason(@Nullable String kickReason) {
        this.kickReason = kickReason;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
