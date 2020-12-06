package de.dytanic.cloudnet.ext.bridge.waterdogpe.event;


import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pe.waterdog.event.Event;
import pe.waterdog.player.ProxiedPlayer;

public class WaterdogPEPlayerFallbackEvent extends Event {

    private final ProxiedPlayer player;
    private ServiceInfoSnapshot fallback;
    private String fallbackName;

    public WaterdogPEPlayerFallbackEvent(ProxiedPlayer player, ServiceInfoSnapshot fallback, String fallbackName) {
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
