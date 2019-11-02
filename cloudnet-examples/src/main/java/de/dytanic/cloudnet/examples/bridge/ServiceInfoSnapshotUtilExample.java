package de.dytanic.cloudnet.examples.bridge;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.PluginInfo;
import de.dytanic.cloudnet.ext.bridge.ServiceInfoSnapshotUtil;

import java.util.Collection;
import java.util.UUID;

public final class ServiceInfoSnapshotUtilExample {

    @EventListener
    public void handle(CloudServiceInfoUpdateEvent event) {
        int onlineCount = ServiceInfoSnapshotUtil.getOnlineCount(event.getServiceInfo()); //The online players count

        int maxPlayers = ServiceInfoSnapshotUtil.getMaxPlayers(event.getServiceInfo()); //The API set PlayerLimit

        String version = ServiceInfoSnapshotUtil.getVersion(event.getServiceInfo()); //The version of the service

        String motd = ServiceInfoSnapshotUtil.getMotd(event.getServiceInfo()); //State motd or null

        String extra = ServiceInfoSnapshotUtil.getExtra(event.getServiceInfo()); //Extra string or null

        String state = ServiceInfoSnapshotUtil.getState(event.getServiceInfo()); //State string or null

        Collection<PluginInfo> pluginInfos = ServiceInfoSnapshotUtil.getPlugins(event.getServiceInfo()); //The pluginInfo items with the important information about the plugin or null

        if (pluginInfos != null) {
            for (PluginInfo pluginInfo : pluginInfos) {
                String pluginInfoName = pluginInfo.getName();
                String pluginInfoVersion = pluginInfo.getVersion();
                JsonDocument subProperties = pluginInfo.getProperties();
            }
        }

        Collection<JsonDocument> players = ServiceInfoSnapshotUtil.getPlayers(event.getServiceInfo());

        if (players != null) {
            for (JsonDocument player : players) {
                UUID uniqueId = player.get("uniqueId", UUID.class);
                String name = player.getString("name");
            }
        }
    }
}