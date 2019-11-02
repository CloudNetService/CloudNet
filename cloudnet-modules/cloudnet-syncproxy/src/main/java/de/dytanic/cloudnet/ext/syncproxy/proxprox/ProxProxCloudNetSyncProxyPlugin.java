package de.dytanic.cloudnet.ext.syncproxy.proxprox;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConfigurationProvider;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.proxprox.listener.ProxProxProxyLoginConfigurationImplListener;
import de.dytanic.cloudnet.ext.syncproxy.proxprox.listener.ProxProxSyncProxyCloudNetListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.proxprox.ProxProx;
import io.gomint.proxprox.api.plugin.Plugin;
import io.gomint.proxprox.api.plugin.annotation.Description;
import io.gomint.proxprox.api.plugin.annotation.Name;
import io.gomint.proxprox.api.plugin.annotation.Version;

import java.util.Map;
import java.util.UUID;

@Name("CloudNet-SyncProxy")
@Version(major = 1, minor = 0)
@Description("CloudNet extension, which implement the multi Proxy server synchronization bridge technology and some small features")
public final class ProxProxCloudNetSyncProxyPlugin extends Plugin {

    private static ProxProxCloudNetSyncProxyPlugin instance;
    private final Map<UUID, Integer> onlineCountOfProxies = Maps.newConcurrentHashMap();


    public ProxProxCloudNetSyncProxyPlugin() {
        instance = this;
    }


    public static ProxProx getProxyServer() {
        return ProxProx.instance;
    }

    public static ProxProxCloudNetSyncProxyPlugin getInstance() {
        return ProxProxCloudNetSyncProxyPlugin.instance;
    }

    @Override
    public void onStartup() {
        initListeners();
        initOnlineCount();
    }

    @Override
    public void onUninstall() {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }

    public boolean inGroup(ServiceInfoSnapshot serviceInfoSnapshot, SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration) {
        Validate.checkNotNull(serviceInfoSnapshot);
        Validate.checkNotNull(syncProxyProxyLoginConfiguration);

        return Iterables.contains(syncProxyProxyLoginConfiguration.getTargetGroup(), serviceInfoSnapshot.getConfiguration().getGroups());
    }

    public SyncProxyProxyLoginConfiguration getProxyLoginConfiguration() {
        for (SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration :
                SyncProxyConfigurationProvider.load().getLoginConfigurations()) {
            if (syncProxyProxyLoginConfiguration.getTargetGroup() != null &&
                    Iterables.contains(syncProxyProxyLoginConfiguration.getTargetGroup(), Wrapper.getInstance().getServiceConfiguration().getGroups())) {
                return syncProxyProxyLoginConfiguration;
            }
        }

        return null;
    }

    public void updateSyncProxyConfigurationInNetwork(SyncProxyConfiguration syncProxyConfiguration) {
        CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
                SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
                new JsonDocument(
                        "syncProxyConfiguration",
                        syncProxyConfiguration
                )
        );
    }

    public int getSyncProxyOnlineCount() {
        int onlinePlayers = getProxyServer().getPlayers().size();

        for (Map.Entry<UUID, Integer> entry : onlineCountOfProxies.entrySet()) {
            if (!Wrapper.getInstance().getServiceId().getUniqueId().equals(entry.getKey())) {
                onlinePlayers += entry.getValue();
            }
        }

        return onlinePlayers;
    }


    private void initListeners() {
        CloudNetDriver.getInstance().getEventManager().registerListener(new ProxProxSyncProxyCloudNetListener());

        registerListener(new ProxProxProxyLoginConfigurationImplListener());
    }

    private void initOnlineCount() {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null && syncProxyProxyLoginConfiguration.getTargetGroup() != null) {
            for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServicesByGroup(syncProxyProxyLoginConfiguration.getTargetGroup())) {
                if ((serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockProxy() ||
                        serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaProxy()) &&
                        serviceInfoSnapshot.getProperties().contains(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT)) {
                    getOnlineCountOfProxies().put(serviceInfoSnapshot.getServiceId().getUniqueId(),
                            serviceInfoSnapshot.getProperties().getInt(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT));
                }
            }
        }
    }

    public Map<UUID, Integer> getOnlineCountOfProxies() {
        return this.onlineCountOfProxies;
    }
}