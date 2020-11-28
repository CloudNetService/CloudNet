package de.dytanic.cloudnet.ext.placeholderapi;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.property.ServiceProperty;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoSnapshotUtil;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.wrapper.Wrapper;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CloudNetPlaceholderExpansion extends PlaceholderExpansion {


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

        // Load the CloudNetDriver
        Optional<CloudNetDriver> optionalCloudNetDriver = CloudNetDriver.optionalInstance();
        // Returns null if the CloudNetDriver isn't loaded
        if(!optionalCloudNetDriver.isPresent()) return null;

        // Load the PlayerManager
        IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
        // Load the Wrapper
        Wrapper wrapper = Wrapper.getInstance();

        
        switch (params) {

            // Returns the name of the current service
            case "service_name":
                return wrapper.getCurrentServiceInfoSnapshot().getName();

            // Returns the name of the current task
            case "task_name":
                return wrapper.getServiceId().getTaskName();

            // Returns the online count of the current task
            case "task_online_count":
                return String.valueOf(ServiceInfoSnapshotUtil.getTaskOnlineCount(wrapper.getServiceId().getTaskName()));

            // Returns the online count of the current service
            case "service_online_count":
                return String.valueOf(wrapper.getCurrentServiceInfoSnapshot().getProperty(BridgeServiceProperty.ONLINE_COUNT));

            // Returns the state of the current service
            case "service_state":
                return wrapper.getCurrentServiceInfoSnapshot().getProperty(BridgeServiceProperty.STATE).orElse("");

            // Returns the node uniqueid of the current service
            case "node_uniqueid":
                return wrapper.getComponentName();

        }
        
        
        return null;
    }
}
