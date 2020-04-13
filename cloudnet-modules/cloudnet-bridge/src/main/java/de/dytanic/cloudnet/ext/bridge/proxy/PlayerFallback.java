package de.dytanic.cloudnet.ext.bridge.proxy;

import com.google.common.collect.ComparisonChain;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import org.jetbrains.annotations.NotNull;

public class PlayerFallback implements Comparable<PlayerFallback> {

    private final int priority;
    private final ServiceInfoSnapshot targetServiceInfoSnapshot;

    public PlayerFallback(int priority, ServiceInfoSnapshot targetServiceInfoSnapshot) {
        this.priority = priority;
        this.targetServiceInfoSnapshot = targetServiceInfoSnapshot;
    }

    public int getPriority() {
        return this.priority;
    }

    public ServiceInfoSnapshot getTarget() {
        return this.targetServiceInfoSnapshot;
    }



    public int getOnlineCount() {
        return this.targetServiceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(-1);
    }

    @Override
    public int compareTo(@NotNull PlayerFallback fallback) {
        return ComparisonChain.start()
                .compare(fallback.priority, this.priority)
                .compare(this.getOnlineCount(), fallback.getOnlineCount())
                .result();
    }
}
