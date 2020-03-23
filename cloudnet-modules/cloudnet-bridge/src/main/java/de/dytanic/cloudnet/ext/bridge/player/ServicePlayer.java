package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetPlayerInfo;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetPlayerInfo;
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetPlayerInfo;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetPlayerInfo;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ServicePlayer {

    private JsonDocument data;

    public ServicePlayer(@NotNull JsonDocument data) {
        this.data = data;
    }

    @NotNull
    public JsonDocument getRawData() {
        return this.data;
    }

    @NotNull
    public UUID getUniqueId() {
        return this.data.get("uniqueId", UUID.class);
    }

    @NotNull
    public String getName() {
        return this.data.getString("name");
    }

    @NotNull
    public BukkitCloudNetPlayerInfo asBukkit() {
        return this.data.toInstanceOf(BukkitCloudNetPlayerInfo.class);
    }

    @NotNull
    public NukkitCloudNetPlayerInfo asNukkit() {
        return this.data.toInstanceOf(NukkitCloudNetPlayerInfo.class);
    }

    @NotNull
    public BungeeCloudNetPlayerInfo asBungee() {
        return this.data.toInstanceOf(BungeeCloudNetPlayerInfo.class);
    }

    @NotNull
    public VelocityCloudNetPlayerInfo asVelocity() {
        return this.data.toInstanceOf(VelocityCloudNetPlayerInfo.class);
    }

}
