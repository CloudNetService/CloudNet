package de.dytanic.cloudnet.ext.syncproxy;


import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import de.dytanic.cloudnet.ext.syncproxy.configuration.*;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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


    protected abstract void schedule(Runnable runnable, long millis);

    public abstract void updateTabList();

    protected abstract void checkWhitelist();

    protected abstract void broadcastServiceStateChange(String key, ServiceInfoSnapshot serviceInfoSnapshot);

    protected String getServiceStateChangeMessage(String key, ServiceInfoSnapshot serviceInfoSnapshot) {
        return this.syncProxyConfiguration.getMessages().get(key)
                .replace("%service%", serviceInfoSnapshot.getServiceId().getName())
                .replace("%node%", serviceInfoSnapshot.getServiceId().getNodeUniqueId());
    }

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

    @Nullable
    public SyncProxyMotd getRandomMotd() {
        if (this.loginConfiguration == null) {
            return null;
        }

        List<SyncProxyMotd> motds = this.loginConfiguration.isMaintenance() ? this.loginConfiguration.getMaintenanceMotds() : this.loginConfiguration.getMotds();

        if (motds == null || motds.isEmpty()) {
            return null;
        }

        return motds.get(RANDOM.nextInt(motds.size()));
    }

    public boolean inGroup(ServiceInfoSnapshot serviceInfoSnapshot) {
        Preconditions.checkNotNull(serviceInfoSnapshot);

        String targetGroup = this.loginConfiguration != null
                ? this.loginConfiguration.getTargetGroup()
                : this.tabListConfiguration != null
                ? this.tabListConfiguration.getTargetGroup()
                : null;

        Preconditions.checkNotNull(targetGroup, "There is no configuration for this proxy group!");

        return Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(targetGroup);
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

            this.schedule(
                    this::scheduleTabList,
                    (long) (1000D / this.tabListConfiguration.getAnimationsPerSecond())
            );

            this.updateTabList();
        } else {
            this.tabListEntryIndex.set(-1);
        }
    }

    public void setSyncProxyConfiguration(SyncProxyConfiguration syncProxyConfiguration) {
        Preconditions.checkNotNull(syncProxyConfiguration, "SyncProxyConfiguration is null!");

        this.syncProxyConfiguration = syncProxyConfiguration;

        this.loginConfiguration = syncProxyConfiguration.getLoginConfigurations().stream()
                .filter(loginConfiguration -> loginConfiguration.getTargetGroup() != null &&
                        Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(loginConfiguration.getTargetGroup()))
                .findFirst().orElse(null);

        this.tabListConfiguration = syncProxyConfiguration.getTabListConfigurations().stream()
                .filter(tabListConfiguration -> tabListConfiguration.getTargetGroup() != null &&
                        Arrays.asList(Wrapper.getInstance().getServiceConfiguration().getGroups()).contains(tabListConfiguration.getTargetGroup()))
                .findFirst().orElse(null);

        if (this.tabListEntryIndex.get() == -1) {
            this.scheduleTabList();
        }

        this.checkWhitelist();
    }

    public SyncProxyConfiguration getSyncProxyConfiguration() {
        return this.syncProxyConfiguration;
    }

    @Nullable
    public SyncProxyProxyLoginConfiguration getLoginConfiguration() {
        return this.loginConfiguration;
    }

    @Nullable
    public SyncProxyTabListConfiguration getTabListConfiguration() {
        return this.tabListConfiguration;
    }

    public AtomicInteger getTabListEntryIndex() {
        return this.tabListEntryIndex;
    }

    @Nullable
    public String getTabListHeader() {
        return this.tabListHeader;
    }

    @Nullable
    public String getTabListFooter() {
        return this.tabListFooter;
    }

}
