package de.dytanic.cloudnet.ext.syncproxy;


import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyTabList;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyTabListConfiguration;
import de.dytanic.cloudnet.wrapper.Wrapper;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractSyncProxyManagement {

    private final AtomicInteger tabListEntryIndex = new AtomicInteger(-1);
    private SyncProxyConfiguration syncProxyConfiguration;
    private SyncProxyProxyLoginConfiguration loginConfiguration;
    private SyncProxyTabListConfiguration tabListConfiguration;
    private String tabListHeader;

    private String tabListFooter;

    public AbstractSyncProxyManagement() {
        this.setSyncProxyConfiguration(this.getConfigurationFromNode());
        this.scheduleTabList();
    }

    protected abstract void scheduleNative(Runnable runnable, long millis);

    public abstract void updateTabList();

    protected abstract String replaceTabListItem(UUID playerUniqueId, SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration, String input);

    protected abstract void checkWhitelist();

    protected abstract void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot);

    public int getSyncProxyOnlineCount() {
        return -1;
    }

    public int getSyncProxyMaxPlayers() {
        return -1;
    }

    public boolean inGroup(ServiceInfoSnapshot serviceInfoSnapshot, SyncProxyProxyLoginConfiguration syncProxyProxyLoginConfiguration) {
        Preconditions.checkNotNull(serviceInfoSnapshot);
        Preconditions.checkNotNull(syncProxyProxyLoginConfiguration);

        return Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(syncProxyProxyLoginConfiguration.getTargetGroup());
    }

    private void scheduleTabList() {
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

    @EventListener
    public void handle(ChannelMessageReceiveEvent event) {
        if (!event.getChannel().equals(SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME)) {
            return;
        }

        if (SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION.equals(event.getMessage().toLowerCase())) {
            SyncProxyConfiguration syncProxyConfiguration = event.getData().get("syncProxyConfiguration", SyncProxyConfiguration.TYPE);

            this.setSyncProxyConfiguration(syncProxyConfiguration);
        }
    }

    public SyncProxyConfiguration getSyncProxyConfiguration() {
        return syncProxyConfiguration;
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
