package de.dytanic.cloudnet.ext.placeholderapi;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.wrapper.Wrapper;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CloudNetPlaceholderExpansion extends PlaceholderExpansion {

    private final Wrapper wrapper;
    private final IPlayerManager playerManager;

    public CloudNetPlaceholderExpansion() {
        this.wrapper = Wrapper.getInstance();
        this.playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cloudnet";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Dytanic";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params) {
            case "service_name":
                return wrapper.getCurrentServiceInfoSnapshot().getName();
            case "task_name":
                return wrapper.getServiceId().getTaskName();
            case "task_online_count":
                return String.valueOf(this.playerManager.taskOnlinePlayers(Wrapper.getInstance().getServiceId().getTaskName()).count());
            case "service_state":
                return wrapper.getCurrentServiceInfoSnapshot().getProperty(BridgeServiceProperty.STATE).orElse("");
        }
        return null;
    }
}
