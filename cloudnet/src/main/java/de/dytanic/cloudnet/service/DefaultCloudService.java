package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.conf.ConfigurationOptionSSL;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientDriverAPI;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.event.service.CloudServicePostPrepareEvent;
import de.dytanic.cloudnet.event.service.CloudServicePrePrepareEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class DefaultCloudService implements ICloudService {

    protected static final String TEMP_NAME_SPLITTER = "_";
    protected static final long SERVICE_ERROR_RESTART_DELAY = 30;
    private static final Lock START_SEQUENCE_LOCK = new ReentrantLock();

    private final String runtime;

    protected volatile ServiceLifeCycle lifeCycle;

    private final ICloudServiceManager cloudServiceManager;
    private final ServiceConfiguration serviceConfiguration;
    protected volatile ServiceInfoSnapshot serviceInfoSnapshot, lastServiceInfoSnapshot;

    private volatile INetworkChannel networkChannel;
    private final String connectionKey;

    protected boolean firstStartupOnStaticService = false;
    private final File directory;

    public DefaultCloudService(String runtime, ICloudServiceManager cloudServiceManager, ServiceConfiguration serviceConfiguration) {
        this.runtime = runtime;
        this.cloudServiceManager = cloudServiceManager;
        this.serviceConfiguration = serviceConfiguration;

        this.serviceInfoSnapshot = this.lastServiceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.DEFINED);

        this.lifeCycle = ServiceLifeCycle.DEFINED;

        this.directory =
                serviceConfiguration.isStaticService() ?
                        new File(cloudServiceManager.getPersistenceServicesDirectory(), this.getServiceId().getName())
                        :
                        new File(cloudServiceManager.getTempDirectory(), this.getServiceId().getName() + TEMP_NAME_SPLITTER + this.getServiceId().getUniqueId().toString());

        this.connectionKey = StringUtil.generateRandomString(256);

        if (this.serviceConfiguration.isStaticService()) {
            this.firstStartupOnStaticService = !this.directory.exists();
        }

        this.directory.mkdirs();

        this.init();
    }

    protected void init() {
        this.initAndPrepareService();
    }

    @NotNull
    @Override
    public String getRuntime() {
        return this.runtime;
    }

    @Override
    public int getConfiguredMaxHeapMemory() {
        return this.serviceConfiguration.getProcessConfig().getMaxHeapMemorySize();
    }

    @Override
    public @NotNull ServiceId getServiceId() {
        return this.serviceConfiguration.getServiceId();
    }

    @NotNull
    public ServiceLifeCycle getLifeCycle() {
        return this.lifeCycle;
    }

    @NotNull
    @Override
    public File getDirectory() {
        return this.directory;
    }

    @NotNull
    @Override
    public ServiceConfiguration getServiceConfiguration() {
        return this.serviceConfiguration;
    }

    @NotNull
    @Override
    public ICloudServiceManager getCloudServiceManager() {
        return this.cloudServiceManager;
    }

    @Override
    public List<String> getGroups() {
        return Arrays.asList(this.serviceConfiguration.getGroups());
    }

    @Override
    public INetworkChannel getNetworkChannel() {
        return this.networkChannel;
    }

    @Override
    public void setNetworkChannel(@NotNull INetworkChannel networkChannel) {
        this.networkChannel = networkChannel;
    }

    @NotNull
    @Override
    public ServiceInfoSnapshot getServiceInfoSnapshot() {
        return this.serviceInfoSnapshot;
    }

    @Override
    public void setServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.lastServiceInfoSnapshot = this.serviceInfoSnapshot;
        this.serviceInfoSnapshot = serviceInfoSnapshot;
    }

    @NotNull
    @Override
    public ServiceInfoSnapshot getLastServiceInfoSnapshot() {
        return this.lastServiceInfoSnapshot;
    }

    @Override
    public String getConnectionKey() {
        return this.connectionKey;
    }

    protected void initAndPrepareService() {
        if (this.lifeCycle == ServiceLifeCycle.DEFINED || this.lifeCycle == ServiceLifeCycle.STOPPED) {
            if (CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePrePrepareEvent(this)).isCancelled()) {
                return;
            }

            System.out.println(LanguageManager.getMessage("cloud-service-pre-prepared-message")
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%id%", this.getServiceId().getUniqueId().toString())
            );

            new File(this.directory, ".wrapper").mkdirs();

            if (CloudNet.getInstance().getConfig().getServerSslConfig().isEnabled()) {
                try {
                    ConfigurationOptionSSL ssl = CloudNet.getInstance().getConfig().getServerSslConfig();

                    File file;
                    if (ssl.getCertificatePath() != null) {
                        file = new File(ssl.getCertificatePath());

                        if (file.exists()) {
                            FileUtils.copy(file, new File(this.directory, ".wrapper/certificate"));
                        }
                    }

                    if (ssl.getPrivateKeyPath() != null) {
                        file = new File(ssl.getPrivateKeyPath());

                        if (file.exists()) {
                            FileUtils.copy(file, new File(this.directory, ".wrapper/privateKey"));
                        }
                    }

                    if (ssl.getTrustCertificatePath() != null) {
                        file = new File(ssl.getTrustCertificatePath());

                        if (file.exists()) {
                            FileUtils.copy(file, new File(this.directory, ".wrapper/trustCertificate"));
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

            this.lifeCycle = ServiceLifeCycle.PREPARED;
            CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePostPrepareEvent(this));

            this.serviceInfoSnapshot.setLifeCycle(ServiceLifeCycle.PREPARED);
            this.cloudServiceManager.getGlobalServiceInfoSnapshots().put(this.getServiceId().getUniqueId(), this.serviceInfoSnapshot);
            CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(this.serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.REGISTER));

            CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-post-prepared-message")
                    .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                    .replace("%task%", this.getServiceId().getTaskName())
                    .replace("%id%", this.getServiceId().getUniqueId().toString())
            );
        }
    }

    protected boolean checkEnoughResources() {
        if (this.cloudServiceManager.getCurrentUsedHeapMemory() + this.getConfiguredMaxHeapMemory() >= CloudNet.getInstance().getConfig().getMaxMemory()) {
            if (CloudNet.getInstance().getConfig().isRunBlockedServiceStartTryLaterAutomatic()) {
                CloudNet.getInstance().runTask(() -> {
                    try {
                        this.start();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
            } else {
                System.out.println(LanguageManager.getMessage("cloud-service-manager-max-memory-error"));
            }

            return false;
        }

        if (CPUUsageResolver.getSystemCPUUsage() >= CloudNet.getInstance().getConfig().getMaxCPUUsageToStartServices()) {
            if (CloudNet.getInstance().getConfig().isRunBlockedServiceStartTryLaterAutomatic()) {
                CloudNet.getInstance().runTask(() -> {
                    try {
                        this.start();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                });
            } else {
                System.out.println(LanguageManager.getMessage("cloud-service-manager-cpu-usage-to-high-error"));
            }

            return false;
        }

        return true;
    }
    
    protected ServiceInfoSnapshot createServiceInfoSnapshot(ServiceLifeCycle lifeCycle) {
        return new ServiceInfoSnapshot(
                System.currentTimeMillis(),
                new HostAndPort(CloudNet.getInstance().getConfig().getHostAddress(), this.serviceConfiguration.getPort()),
                -1,
                lifeCycle,
                this.serviceInfoSnapshot != null ? this.serviceInfoSnapshot.getProcessSnapshot() : ProcessSnapshot.empty(),
                this.serviceInfoSnapshot != null ? this.serviceInfoSnapshot.getProperties() : this.serviceConfiguration.getProperties(),
                this.serviceConfiguration
        );
    }

    @Override
    public ITask<ServiceInfoSnapshot> forceUpdateServiceInfoSnapshotAsync() {
        if (this.getNetworkChannel() == null) {
            return CompletedTask.create(null);
        }

        return this.getNetworkChannel()
                .sendQueryAsync(new PacketClientDriverAPI(DriverAPIRequestType.FORCE_UPDATE_SERVICE))
                .map(packet -> packet.getBuffer().readObject(ServiceInfoSnapshot.class))
                .onComplete(serviceInfoSnapshot -> {
                    if (serviceInfoSnapshot != null) {
                        this.updateServiceInfoSnapshot(serviceInfoSnapshot);
                    }
                });
    }

    @Override
    public void updateServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
        this.setServiceInfoSnapshot(serviceInfoSnapshot);
        this.getCloudServiceManager().getGlobalServiceInfoSnapshots().put(serviceInfoSnapshot.getServiceId().getUniqueId(), serviceInfoSnapshot);

        CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceInfoUpdateEvent(serviceInfoSnapshot));

        CloudNet.getInstance().getNetworkClient()
                .sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UPDATE)); // TODO: is this really necessary?
        CloudNet.getInstance().getNetworkServer()
                .sendPacket(new PacketClientServerServiceInfoPublisher(serviceInfoSnapshot, PacketClientServerServiceInfoPublisher.PublisherType.UPDATE));
    }

    @Override
    public int stop() {
        return this.shutdown(false);
    }

    @Override
    public int kill() {
        return this.shutdown(true);
    }

    protected abstract int shutdown(boolean force);

    @Override
    public void start() throws Exception {
        if (!CloudNet.getInstance().getConfig().isParallelServiceStartSequence()) {
            try {

                START_SEQUENCE_LOCK.lock();
                this.startNow();
            } finally {
                START_SEQUENCE_LOCK.unlock();
            }
        } else {
            this.startNow();
        }
    }

    protected abstract void startNow() throws Exception;

}
