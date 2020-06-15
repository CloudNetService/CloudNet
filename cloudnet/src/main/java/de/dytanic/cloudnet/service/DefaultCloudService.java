package de.dytanic.cloudnet.service;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
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
import de.dytanic.cloudnet.event.service.*;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class DefaultCloudService implements ICloudService {

    protected static final String TEMP_NAME_SPLITTER = "_";

    protected static final long SERVICE_ERROR_RESTART_DELAY = 30;

    private final String runtime;

    private final List<ServiceRemoteInclusion> includes = new ArrayList<>();
    private final List<ServiceTemplate> templates = new ArrayList<>();
    private final List<ServiceDeployment> deployments = new CopyOnWriteArrayList<>();
    private final Queue<ServiceRemoteInclusion> waitingIncludes = new ConcurrentLinkedQueue<>();
    private final Queue<ServiceTemplate> waitingTemplates = new ConcurrentLinkedQueue<>();

    protected volatile ServiceLifeCycle lifeCycle;

    private final ICloudServiceManager cloudServiceManager;
    private final ServiceConfiguration serviceConfiguration;
    protected volatile ServiceInfoSnapshot serviceInfoSnapshot, lastServiceInfoSnapshot;

    private volatile INetworkChannel networkChannel;
    private final String connectionKey;

    private boolean firstStartupOnStaticService = false;

    private final File directory;

    public DefaultCloudService(String runtime, ICloudServiceManager cloudServiceManager, ServiceConfiguration serviceConfiguration) {
        this.runtime = runtime;
        this.cloudServiceManager = cloudServiceManager;
        this.serviceConfiguration = serviceConfiguration;

        this.serviceInfoSnapshot = this.lastServiceInfoSnapshot = this.createServiceInfoSnapshot(ServiceLifeCycle.DEFINED);

        this.lifeCycle = ServiceLifeCycle.DEFINED;

        this.waitingIncludes.addAll(Arrays.asList(this.serviceConfiguration.getIncludes()));
        this.waitingTemplates.addAll(Arrays.asList(this.serviceConfiguration.getTemplates()));
        this.deployments.addAll(Arrays.asList(this.serviceConfiguration.getDeployments()));

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

    @Override
    public List<ServiceDeployment> getDeployments() {
        return this.deployments;
    }

    @Override
    public Queue<ServiceRemoteInclusion> getWaitingIncludes() {
        return this.waitingIncludes;
    }

    @Override
    public Queue<ServiceTemplate> getWaitingTemplates() {
        return this.waitingTemplates;
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

    @Override
    public List<ServiceRemoteInclusion> getIncludes() {
        return Collections.unmodifiableList(this.includes);
    }

    @Override
    public List<ServiceTemplate> getTemplates() {
        return Collections.unmodifiableList(this.templates);
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
        this.serviceConfiguration.setDeployments(this.deployments.toArray(new ServiceDeployment[0]));
        this.serviceConfiguration.setTemplates(this.templates.toArray(new ServiceTemplate[0]));
        this.serviceConfiguration.setIncludes(this.includes.toArray(new ServiceRemoteInclusion[0]));

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

    private ITemplateStorage getStorage(String storageName) {
        ITemplateStorage storage;

        if (CloudNetDriver.getInstance().getServicesRegistry().containsService(ITemplateStorage.class, storageName)) {
            storage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, storageName);
        } else {
            storage = CloudNetDriver.getInstance().getServicesRegistry().getService(ITemplateStorage.class, LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);
        }

        return storage;
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
    public void includeInclusions() {
        byte[] buffer = new byte[16384];

        while (!this.waitingIncludes.isEmpty()) {
            ServiceRemoteInclusion inclusion = this.waitingIncludes.poll();

            if (inclusion != null && inclusion.getDestination() != null && inclusion.getUrl() != null) {
                try {
                    CloudNet.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-include-inclusion-message")
                            .replace("%task%", this.getServiceId().getTaskName())
                            .replace("%id%", this.getServiceId().getUniqueId().toString())
                            .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                            .replace("%url%", inclusion.getUrl())
                            .replace("%destination%", inclusion.getDestination())
                    );

                    File cacheDestination = new File(
                            System.getProperty("cloudnet.tempDir.includes", "temp/includes"),
                            Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(inclusion.getUrl()))
                    );
                    cacheDestination.getParentFile().mkdirs();

                    if (!cacheDestination.exists()) {
                        if (!this.includeInclusions0(inclusion, cacheDestination, buffer)) {
                            continue;
                        }
                    }

                    try (InputStream inputStream = new FileInputStream(cacheDestination)) {
                        File destination = new File(this.directory, inclusion.getDestination());
                        destination.getParentFile().mkdirs();

                        try (OutputStream outputStream = Files.newOutputStream(destination.toPath())) {
                            FileUtils.copy(inputStream, outputStream, buffer);
                        }
                    }

                    this.includes.add(inclusion);

                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private boolean includeInclusions0(ServiceRemoteInclusion inclusion, File destination, byte[] buffer) throws Exception {
        URLConnection connection = new URL(inclusion.getUrl()).openConnection();

        if (CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServicePreLoadInclusionEvent(this, inclusion, connection)).isCancelled()) {
            return false;
        }

        if (inclusion.getProperties() != null) {
            if (inclusion.getProperties().contains("httpHeaders")) {
                JsonDocument document = inclusion.getProperties().getDocument("httpHeaders");

                for (String key : document) {
                    connection.setRequestProperty(key, document.get(key).toString());
                }
            }
        }

        connection.setDoOutput(false);
        connection.setUseCaches(false);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

        connection.connect();

        try (InputStream inputStream = connection.getInputStream(); OutputStream outputStream = Files.newOutputStream(destination.toPath())) {
            FileUtils.copy(inputStream, outputStream, buffer);
        }

        return true;
    }

    @Override
    public void includeTemplates() {
        while (!this.waitingTemplates.isEmpty()) {
            ServiceTemplate template = this.waitingTemplates.poll();

            if (template != null && template.getName() != null && template.getPrefix() != null && template.getStorage() != null) {
                ITemplateStorage storage = this.getStorage(template.getStorage());

                if (!storage.has(template)) {
                    continue;
                }

                CloudServiceTemplateLoadEvent cloudServiceTemplateLoadEvent = new CloudServiceTemplateLoadEvent(this, storage, template);
                CloudNetDriver.getInstance().getEventManager().callEvent(cloudServiceTemplateLoadEvent);

                if (cloudServiceTemplateLoadEvent.isCancelled()) {
                    continue;
                }

                try {
                    if (!this.serviceConfiguration.isStaticService() || template.shouldAlwaysCopyToStaticServices() || this.firstStartupOnStaticService) {
                        CloudNet.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-include-template-message")
                                .replace("%task%", this.getServiceId().getTaskName())
                                .replace("%id%", this.getServiceId().getUniqueId().toString())
                                .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                                .replace("%template%", template.getTemplatePath())
                                .replace("%storage%", template.getStorage())
                        );

                        storage.copy(template, this.directory);
                    }

                    this.templates.add(template);

                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }

    }

    @Override
    public void deployResources(boolean removeDeployments) {
        for (ServiceDeployment deployment : this.deployments) {
            if (deployment != null) {
                if (deployment.getTemplate() != null && deployment.getTemplate().getStorage() != null && deployment.getTemplate().getPrefix() != null &&
                        deployment.getTemplate().getName() != null) {
                    ITemplateStorage storage = this.getStorage(deployment.getTemplate().getStorage());

                    CloudServiceDeploymentEvent cloudServiceDeploymentEvent = new CloudServiceDeploymentEvent(this, storage, deployment);
                    CloudNetDriver.getInstance().getEventManager().callEvent(cloudServiceDeploymentEvent);

                    if (cloudServiceDeploymentEvent.isCancelled()) {
                        continue;
                    }

                    System.out.println(LanguageManager.getMessage("cloud-service-deploy-message")
                            .replace("%task%", this.getServiceId().getTaskName())
                            .replace("%id%", this.getServiceId().getUniqueId().toString())
                            .replace("%serviceId%", String.valueOf(this.getServiceId().getTaskServiceId()))
                            .replace("%template%", deployment.getTemplate().getTemplatePath())
                            .replace("%storage%", deployment.getTemplate().getStorage())
                    );

                    storage.deploy(this.directory, deployment.getTemplate(), pathname -> {
                                if (deployment.getExcludes() != null) {
                                    return !deployment.getExcludes().contains(pathname.isDirectory() ? pathname.getName() + "/" : pathname.getName()) && !pathname
                                            .getName().equals("wrapper.jar") && !pathname.getName().equals(".wrapper");
                                } else {
                                    return true;
                                }
                            }
                    );

                    if (removeDeployments) {
                        this.deployments.remove(deployment);
                    }

                    if (storage instanceof LocalTemplateStorage) {
                        CloudNet.getInstance().deployTemplateInCluster(deployment.getTemplate(), storage.toZipByteArray(deployment.getTemplate()));
                    }
                }
            }
        }
    }

    @Override
    public void offerTemplate(@NotNull ServiceTemplate template) {
        this.waitingTemplates.offer(template);
        this.updateServiceInfoSnapshot(this.createServiceInfoSnapshot(this.lifeCycle));
    }

    @Override
    public void offerInclusion(@NotNull ServiceRemoteInclusion inclusion) {
        this.waitingIncludes.offer(inclusion);
        this.updateServiceInfoSnapshot(this.createServiceInfoSnapshot(this.lifeCycle));
    }

    @Override
    public void addDeployment(@NotNull ServiceDeployment deployment) {
        this.deployments.add(deployment);
        this.updateServiceInfoSnapshot(this.createServiceInfoSnapshot(this.lifeCycle));
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

}
