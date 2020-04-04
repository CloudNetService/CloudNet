package de.dytanic.cloudnet.examples.bridge;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.player.ServicePlayer;

import java.util.UUID;

public final class ServiceInfoSnapshotUtilExample {

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        int onlineCount = event.getServiceInfo().getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0); //The online players count

        int maxPlayers = event.getServiceInfo().getProperty(BridgeServiceProperty.MAX_PLAYERS).orElse(0); //The API set PlayerLimit

        String version = event.getServiceInfo().getProperty(BridgeServiceProperty.VERSION).orElse(null); //The version of the service

        String motd = event.getServiceInfo().getProperty(BridgeServiceProperty.MOTD).orElse(null); //State motd or null

        String extra = event.getServiceInfo().getProperty(BridgeServiceProperty.EXTRA).orElse(null); //Extra string or null

        String state = event.getServiceInfo().getProperty(BridgeServiceProperty.STATE).orElse(null); //State string or null

        event.getServiceInfo().getProperty(BridgeServiceProperty.PLUGINS).ifPresent(pluginInfos -> { //The pluginInfo items with the important information about the plugin
            for (PluginInfo pluginInfo : pluginInfos) {
                String pluginInfoName = pluginInfo.getName();
                String pluginInfoVersion = pluginInfo.getVersion();
                JsonDocument subProperties = pluginInfo.getProperties();
            }
        });

        event.getServiceInfo().getProperty(BridgeServiceProperty.PLAYERS).ifPresent(players -> {
            for (ServicePlayer player : players) {
                UUID uniqueId = player.getUniqueId();
                String name = player.getName();
            }
        });

    }
}