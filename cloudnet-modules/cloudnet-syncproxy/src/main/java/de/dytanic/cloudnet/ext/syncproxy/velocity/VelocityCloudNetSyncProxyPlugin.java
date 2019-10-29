package de.dytanic.cloudnet.ext.syncproxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Maps;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.syncproxy.*;
import de.dytanic.cloudnet.ext.syncproxy.velocity.listener.VelocityProxyLoginConfigurationImplListener;
import de.dytanic.cloudnet.ext.syncproxy.velocity.listener.VelocityProxyTabListConfigurationImplListener;
import de.dytanic.cloudnet.ext.syncproxy.velocity.listener.VelocitySyncProxyCloudNetListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import net.kyori.text.TextComponent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(id = "cloudnet_syncproxy_velocity")
public final class VelocityCloudNetSyncProxyPlugin {

    private static VelocityCloudNetSyncProxyPlugin instance;

    private final ProxyServer proxyServer;


    private final Map<UUID, Integer> onlineCountOfProxies = Maps.newConcurrentHashMap();


    private volatile AtomicInteger tabListEntryIndex = new AtomicInteger(-1);

    private volatile String tabListHeader = null, tabListFooter = null;

    @Inject
    public VelocityCloudNetSyncProxyPlugin(ProxyServer proxyServer) {
        instance = this;

        this.proxyServer = proxyServer;
    }

    public static VelocityCloudNetSyncProxyPlugin getInstance() {
        return VelocityCloudNetSyncProxyPlugin.instance;
    }

    @Subscribe
    public void handleProxyInit(ProxyInitializeEvent event) {
        initListeners();
        initOnlineCount();
        scheduleTabList();
    }

    @Subscribe
    public void handleProxyShutdown(ProxyShutdownEvent event) {
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(getClass().getClassLoader());
        Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
    }


    public int getSyncProxyOnlineCount() {
        int onlinePlayers = proxyServer.getPlayerCount();

        for (Map.Entry<UUID, Integer> entry : onlineCountOfProxies.entrySet()) {
            if (!Wrapper.getInstance().getServiceId().getUniqueId().equals(entry.getKey())) {
                onlinePlayers += entry.getValue();
            }
        }

        return onlinePlayers;
    }

    public void updateSyncProxyConfigurationInNetwork(SyncProxyConfiguration syncProxyConfiguration) {
        CloudNetDriver.getInstance().sendChannelMessage(
                SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
                SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
                new JsonDocument(
                        "syncProxyConfiguration",
                        syncProxyConfiguration
                )
        );
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

    public SyncProxyTabListConfiguration getTabListConfiguration() {
        for (SyncProxyTabListConfiguration syncProxyTabListConfiguration :
                SyncProxyConfigurationProvider.load().getTabListConfigurations()) {
            if (syncProxyTabListConfiguration.getTargetGroup() != null &&
                    Iterables.contains(syncProxyTabListConfiguration.getTargetGroup(), Wrapper.getInstance().getServiceConfiguration().getGroups())) {
                return syncProxyTabListConfiguration;
            }
        }

        return null;
    }

    public void setTabList(Player player) {
        if (tabListEntryIndex.get() == -1) {
            return;
        }

        SyncProxyProxyLoginConfiguration proxyProxyLoginConfiguration = getProxyLoginConfiguration();

        player.getTabList().setHeaderAndFooter(
                TextComponent.of(tabListHeader != null ? replaceTabListItem(player, proxyProxyLoginConfiguration, tabListHeader) : ""),
                TextComponent.of(tabListFooter != null ? replaceTabListItem(player, proxyProxyLoginConfiguration, tabListFooter) : "")
        );
    }

    private String replaceTabListItem(Player player, SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration, String input) {
        input = input
                .replace("%server%", player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "")
                .replace("%online_players%", String.valueOf(syncProxyProxyLoginConfiguration != null ? getSyncProxyOnlineCount() : proxyServer.getPlayerCount()))
                .replace("%max_players%", String.valueOf(syncProxyProxyLoginConfiguration != null ? syncProxyProxyLoginConfiguration.getMaxPlayers() : proxyServer.getConfiguration().getShowMaxPlayers()))
                .replace("%name%", player.getUsername())
                .replace("%ping%", String.valueOf(player.getPing()));

        return SyncProxyTabList.replaceTabListItem(input, player.getUniqueId());
    }


    private void initListeners() {
        //Velocity
        proxyServer.getEventManager().register(this, new VelocityProxyLoginConfigurationImplListener());
        proxyServer.getEventManager().register(this, new VelocityProxyTabListConfigurationImplListener());

        //CloudNet
        CloudNetDriver.getInstance().getEventManager().registerListener(new VelocitySyncProxyCloudNetListener());
    }

    private void scheduleTabList() {
        SyncProxyTabListConfiguration syncProxyTabListConfiguration = getTabListConfiguration();

        if (syncProxyTabListConfiguration != null && syncProxyTabListConfiguration.getEntries() != null &&
                !syncProxyTabListConfiguration.getEntries().isEmpty()) {
            if (tabListEntryIndex.get() == -1) {
                tabListEntryIndex.set(0);
            }

            if ((tabListEntryIndex.get() + 1) < syncProxyTabListConfiguration.getEntries().size()) {
                tabListEntryIndex.incrementAndGet();
            } else {
                tabListEntryIndex.set(0);
            }

            SyncProxyTabList tabList = syncProxyTabListConfiguration.getEntries().get(tabListEntryIndex.get());

            tabListHeader = tabList.getHeader();
            tabListFooter = tabList.getFooter();

            proxyServer.getScheduler()
                    .buildTask(this, this::scheduleTabList)
                    .delay(1000 / syncProxyTabListConfiguration.getAnimationsPerSecond(), TimeUnit.MILLISECONDS)
                    .schedule();
        } else {
            tabListEntryIndex.set(-1);
            proxyServer.getScheduler()
                    .buildTask(this, this::scheduleTabList)
                    .delay(500, TimeUnit.MILLISECONDS)
                    .schedule();
        }

        for (Player player : proxyServer.getAllPlayers()) {
            if (player.isActive() && player.getCurrentServer().isPresent()) {
                setTabList(player);
            }
        }
    }

    private void initOnlineCount() {
        SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration = getProxyLoginConfiguration();

        if (syncProxyProxyLoginConfiguration != null && syncProxyProxyLoginConfiguration.getTargetGroup() != null) {
            for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceByGroup(syncProxyProxyLoginConfiguration.getTargetGroup())) {
                if ((serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftBedrockProxy() ||
                        serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftJavaProxy()) &&
                        serviceInfoSnapshot.getProperties().contains(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT)) {
                    getOnlineCountOfProxies().put(serviceInfoSnapshot.getServiceId().getUniqueId(),
                            serviceInfoSnapshot.getProperties().getInt(SyncProxyConstants.SYNC_PROXY_SERVICE_INFO_SNAPSHOT_ONLINE_COUNT));
                }
            }
        }
    }

    public ProxyServer getProxyServer() {
        return this.proxyServer;
    }

    public Map<UUID, Integer> getOnlineCountOfProxies() {
        return this.onlineCountOfProxies;
    }

    public AtomicInteger getTabListEntryIndex() {
        return this.tabListEntryIndex;
    }

    public String getTabListHeader() {
        return this.tabListHeader;
    }

    public String getTabListFooter() {
        return this.tabListFooter;
    }
}