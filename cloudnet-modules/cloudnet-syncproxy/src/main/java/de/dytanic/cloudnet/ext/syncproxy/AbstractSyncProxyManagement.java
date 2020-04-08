package de.dytanic.cloudnet.ext.syncproxy;


import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.syncproxy.configuration.*;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractSyncProxyManagement {

    private static final Random RANDOM = new Random();
    protected final AtomicInteger tabListEntryIndex = new AtomicInteger(-1);
    protected IPlayerManager playerManager = CloudNetDriver.getInstance().getServicesRegistry().getFirstService(IPlayerManager.class);
    protected SyncProxyConfiguration syncProxyConfiguration;

    protected SyncProxyProxyLoginConfiguration loginConfiguration;

    protected SyncProxyTabListConfiguration tabListConfiguration;

    protected String tabListHeader;

    protected String tabListFooter;

    protected Map<UUID, Integer> onlineCountCache = new HashMap<>();


    public AbstractSyncProxyManagement() {
        this.setSyncProxyConfiguration(this.getConfigurationFromNode());
    }


    protected abstract void scheduleNative(Runnable runnable, long millis);

    public abstract void updateTabList();

    protected abstract void checkWhitelist();

    protected abstract void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot);

    protected void updateServiceOnlineCount(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.onlineCountCache.put(
                serviceInfoSnapshot.getServiceId().getUniqueId(),
                serviceInfoSnapshot.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0)
        );
        this.updateTabList();
    }

    protected void removeServiceOnlineCount(ServiceInfoSnapshot serviceInfoSnapshot) {
        this.onlineCountCache.remove(serviceInfoSnapshot.getServiceId().getUniqueId());
        this.updateTabList();
    }

    public int getSyncProxyOnlineCount() {
        return this.onlineCountCache.values().stream()
                .mapToInt(count -> count)
                .sum();
    }

    public SyncProxyMotd getRandomMotd() {
        if (this.loginConfiguration.isMaintenance()) {
            if (this.loginConfiguration.getMaintenanceMotds() != null && !this.loginConfiguration.getMaintenanceMotds().isEmpty()) {
                return this.loginConfiguration.getMaintenanceMotds().get(RANDOM.nextInt(this.loginConfiguration.getMaintenanceMotds().size()));
            }
        } else {
            if (this.loginConfiguration.getMotds() != null && !this.loginConfiguration.getMotds().isEmpty()) {
                return this.loginConfiguration.getMotds().get(RANDOM.nextInt(this.loginConfiguration.getMotds().size()));
            }
        }

        return null;
    }

    public boolean inGroup(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);
        Preconditions.checkNotNull(this.loginConfiguration);

        return Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(this.loginConfiguration.getTargetGroup());
    }

    protected void scheduleTabList() {
        if (this.tabListConfiguration != null && this.tabListConfiguration.getEntries() != null &&
                !this.tabListConfiguration.getEntries().isEmpty()) {
            if (this.tabListEntryIndex.get() == -1) {
                this.tabListEntryIndex.set(0);
            }

            if ((this.tabListEntryIndex.get() + 1) < this.tabListConfiguration.getEntries().size()) {
                this.tabListEntryIndex.incrementAndGet();
            } else {
                this.tabListEntryIndex.set(0);
            }

            SyncProxyTabList tabList = this.tabListConfiguration.getEntries().get(this.tabListEntryIndex.get());

            this.tabListHeader = tabList.getHeader();
            this.tabListFooter = tabList.getFooter();

            this.scheduleNative(
                    this::scheduleTabList,
                    (long) (1000D / this.tabListConfiguration.getAnimationsPerSecond())
            );
        } else {
            this.tabListEntryIndex.set(-1);
            this.scheduleNative(this::scheduleTabList, 500);
        }

        this.updateTabList();
    }

    private SyncProxyConfiguration getConfigurationFromNode() {
        ITask<SyncProxyConfiguration> task = CloudNetDriver.getInstance().getPacketQueryProvider().sendCallablePacket(CloudNetDriver.getInstance().getNetworkClient().getChannels().iterator().next(),
                SyncProxyConstants.SYNC_PROXY_SYNC_CHANNEL_PROPERTY,
                SyncProxyConstants.SIGN_CHANNEL_SYNC_ID_GET_SYNC_PROXY_CONFIGURATION_PROPERTY,
                new JsonDocument(),
                documentPair -> documentPair.get("syncProxyConfiguration", SyncProxyConfiguration.TYPE));

        try {
            return task.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
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

    public void setSyncProxyConfiguration(SyncProxyConfiguration syncProxyConfiguration) {
        if (syncProxyConfiguration != null) {
            this.syncProxyConfiguration = syncProxyConfiguration;

            this.loginConfiguration = syncProxyConfiguration.getLoginConfigurations().stream()
                    .filter(loginConfiguration -> loginConfiguration.getTargetGroup() != null &&
                            Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(loginConfiguration.getTargetGroup()))
                    .findFirst().orElse(null);

            this.tabListConfiguration = syncProxyConfiguration.getTabListConfigurations().stream()
                    .filter(tabListConfiguration -> tabListConfiguration.getTargetGroup() != null &&
                            Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(tabListConfiguration.getTargetGroup()))
                    .findFirst().orElse(null);

            this.checkWhitelist();
        }
    }

    public SyncProxyConfiguration getSyncProxyConfiguration() {
        return syncProxyConfiguration;
    }

    public SyncProxyProxyLoginConfiguration getLoginConfiguration() {
        return loginConfiguration;
    }

    public SyncProxyTabListConfiguration getTabListConfiguration() {
        return tabListConfiguration;
    }

    public AtomicInteger getTabListEntryIndex() {
        return tabListEntryIndex;
    }

    public String getTabListHeader() {
        return tabListHeader;
    }

    public String getTabListFooter() {
        return tabListFooter;
    }

}
