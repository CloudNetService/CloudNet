package de.dytanic.cloudnet.ext.bridge.bungee.event;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BungeePlayerFallbackEvent extends BungeeCloudNetEvent {

    private final ProxiedPlayer player;
    private ServiceInfoSnapshot fallback;
    private String fallbackName;

    public BungeePlayerFallbackEvent(ProxiedPlayer player, ServiceInfoSnapshot fallback, String fallbackName) {
        this.player = player;
        this.fallback = fallback;
        this.fallbackName = fallbackName;
    }

    @NotNull
    public ProxiedPlayer getPlayer() {
        return this.player;
    }

    @Nullable
    public String getFallbackName() {
        return this.fallbackName != null ? this.fallbackName : this.fallback != null ? this.fallback.getServiceId().getName() : null;
    }

    @Nullable
    public ServiceInfoSnapshot getFallback() {
        return this.fallback;
    }

    public void setFallback(@Nullable ServiceInfoSnapshot fallback) {
        this.fallback = fallback;
        this.fallbackName = null;
    }

    public void setFallback(@Nullable String name) {
        this.fallbackName = name;
        this.fallback = null;
    }

}
