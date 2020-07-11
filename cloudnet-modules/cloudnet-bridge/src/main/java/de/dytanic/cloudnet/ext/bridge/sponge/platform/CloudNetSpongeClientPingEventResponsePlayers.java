package de.dytanic.cloudnet.ext.bridge.sponge.platform;

import de.dytanic.cloudnet.ext.bridge.server.BridgeServerHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.profile.GameProfile;

import java.util.ArrayList;
import java.util.List;

class CloudNetSpongeClientPingEventResponsePlayers implements ClientPingServerEvent.Response.Players {

    static @NotNull ClientPingServerEvent.Response.Players fromCloudNet() {
        return new CloudNetSpongeClientPingEventResponsePlayers(
                Sponge.getServer().getOnlinePlayers().size(),
                BridgeServerHelper.getMaxPlayers()
        );
    }

    private CloudNetSpongeClientPingEventResponsePlayers(int online, int max) {
        this.online = online;
        this.max = max;
    }

    private int online;
    private int max;
    private final List<GameProfile> gameProfiles = new ArrayList<>(Sponge.getServer().getGameProfileManager().getCache().getProfiles());

    @Override
    public void setOnline(int online) {
        this.online = online;
    }

    @Override
    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public int getOnline() {
        return this.online;
    }

    @Override
    public int getMax() {
        return this.max;
    }

    @Override
    public @NotNull List<GameProfile> getProfiles() {
        return this.gameProfiles;
    }
}
