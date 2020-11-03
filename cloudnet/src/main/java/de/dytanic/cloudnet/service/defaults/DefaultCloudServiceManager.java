package de.dytanic.cloudnet.service.defaults;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ThrowableConsumer;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.event.service.CloudServiceCreateEvent;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.handler.CloudServiceHandler;
import de.dytanic.cloudnet.service.handler.DefaultCloudServiceHandler;
import de.dytanic.cloudnet.util.PortValidator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class DefaultCloudServiceManager implements ICloudServiceManager {

    private static final CloudServiceHandler HANDLER = DefaultCloudServiceHandler.INSTANCE;
    private static final ICloudServiceFactory DEFAULT_FACTORY = new DefaultCloudServiceFactory(JVMCloudService.RUNTIME, (manager, configuration) -> new JVMCloudService(manager, configuration, HANDLER));
    private final File tempDirectory = new File(System.getProperty("cloudnet.tempDir.services", "temp/services"));
    private final File persistenceServicesDirectory = new File(System.getProperty("cloudnet.persistable.services.path", "local/services"));
    private final Map<UUID, ServiceInfoSnapshot> globalServiceInfoSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, ICloudService> cloudServices = new ConcurrentHashMap<>();
    private final Map<String, ICloudServiceFactory> cloudServiceFactories = new ConcurrentHashMap<>();

    @Override
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    public ICloudService runTask(@NotNull ServiceConfiguration serviceConfiguration) {
        try {
            return this.createCloudService(serviceConfiguration).get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Override
    @ApiStatus.Internal
    public ITask<ICloudService> createCloudService(@NotNull ServiceConfiguration serviceConfiguration) {
        if (CloudNet.getInstance().isMainThread()) {
            return CompletedTask.create(this.createCloudServiceSync(serviceConfiguration));
        } else {
            return CloudNet.getInstance().runTask(() -> createCloudServiceSync(serviceConfiguration));
        }
    }

    private ICloudService createCloudServiceSync(@NotNull ServiceConfiguration serviceConfiguration) {
        this.prepareServiceConfiguration(serviceConfiguration);

        CloudServiceCreateEvent event = new CloudServiceCreateEvent(serviceConfiguration);
        CloudNetDriver.getInstance().getEventManager().callEvent(event);

        if (event.isCancelled()) {
            return null;
        }

        serviceConfiguration.setPort(this.checkAndReplacePort(serviceConfiguration.getPort()));
        serviceConfiguration.getServiceId().setTaskServiceId(this.checkAndReplaceTaskId(serviceConfiguration.getServiceId()));

        ICloudService cloudService = this.getCloudServiceFactory(serviceConfiguration.getRuntime())
                .map(factory -> factory.createCloudService(this, serviceConfiguration))
                .orElseGet(() -> DEFAULT_FACTORY.createCloudService(this, serviceConfiguration));

        if (cloudService != null) {
            cloudService.init();

            this.cloudServices.put(cloudService.getServiceId().getUniqueId(), cloudService);
            this.globalServiceInfoSnapshots.put(cloudService.getServiceId().getUniqueId(), cloudService.getServiceInfoSnapshot());

            CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(cloudService.getServiceInfoSnapshot(), PacketClientServerServiceInfoPublisher.PublisherType.REGISTER));
            CloudNet.getInstance().publishNetworkClusterNodeInfoSnapshotUpdate();
        }

        return cloudService;
    }

    private void prepareServiceConfiguration(ServiceConfiguration configuration) {
        configuration.getServiceId().setNodeUniqueId(CloudNet.getInstance().getComponentName());

        Collection<String> groups = new ArrayList<>(Arrays.asList(configuration.getGroups()));

        Collection<ServiceTemplate> templates = new ArrayList<>();
        Collection<ServiceDeployment> deployments = new ArrayList<>();
        Collection<ServiceRemoteInclusion> inclusions = new ArrayList<>();

        for (GroupConfiguration groupConfiguration : CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations()) {
            String groupName = groupConfiguration.getName();

            if (!groups.contains(groupName) && groupConfiguration.getTargetEnvironments().contains(configuration.getProcessConfig().getEnvironment())) {
                groups.add(groupName);
            }

            if (groups.contains(groupName)) {
                inclusions.addAll(groupConfiguration.getIncludes());
                templates.addAll(groupConfiguration.getTemplates());
                deployments.addAll(groupConfiguration.getDeployments());

                configuration.getProcessConfig().getJvmOptions().addAll(groupConfiguration.getJvmOptions());
                configuration.getProcessConfig().getProcessParameters().addAll(groupConfiguration.getProcessParameters());

                configuration.getProperties().append(groupConfiguration.getProperties());
            }
        }

        // adding the task templates after the group templates for them to have a higher priority
        templates.addAll(Arrays.asList(configuration.getTemplates()));
        deployments.addAll(Arrays.asList(configuration.getDeployments()));
        inclusions.addAll(Arrays.asList(configuration.getIncludes()));

        configuration.setTemplates(templates.toArray(new ServiceTemplate[0]));
        configuration.setDeployments(deployments.toArray(new ServiceDeployment[0]));
        configuration.setIncludes(inclusions.toArray(new ServiceRemoteInclusion[0]));
    }

    @Override
    public void startAllCloudServices() {
        this.executeForAllServices(ICloudService::start);
    }

    @Override
    public void stopAllCloudServices() {
        this.executeForAllServices(ICloudService::stop);
    }

    @Override
    public void deleteAllCloudServices() {
        this.executeForAllServices(ICloudService::delete);
    }

    private void executeForAllServices(ThrowableConsumer<ICloudService, Exception> consumer) {
        if (this.cloudServices.isEmpty()) {
            return;
        }
        Collection<ICloudService> cloudServices = new ArrayList<>(this.cloudServices.values());

        CountDownLatch countDownLatch = new CountDownLatch(cloudServices.size());
        ExecutorService executorService = Executors.newFixedThreadPool((cloudServices.size() / 2) + 1);

        for (ICloudService cloudService : cloudServices) {
            executorService.execute(() -> {
                try {
                    consumer.accept(cloudService);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                countDownLatch.countDown();
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        executorService.shutdownNow();
    }

    @Nullable
    @Override
    public ICloudService getCloudService(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.cloudServices.get(uniqueId);
    }

    @Nullable
    @Override
    public ICloudService getCloudService(@NotNull Predicate<ICloudService> predicate) {
        Preconditions.checkNotNull(predicate);

        return this.cloudServices.values().stream().filter(predicate).findFirst().orElse(null);
    }

    @Override
    public Collection<ICloudService> getLocalCloudServices(@NotNull String taskName) {
        Preconditions.checkNotNull(taskName);

        return this.cloudServices.values().stream().filter(iCloudService -> iCloudService.getServiceId().getTaskName().equalsIgnoreCase(taskName)).collect(Collectors.toList());
    }

    @Override
    public Collection<ICloudService> getLocalCloudServices(@NotNull Predicate<ICloudService> predicate) {
        Preconditions.checkNotNull(predicate);

        return this.cloudServices.values().stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public Collection<ICloudService> getLocalCloudServices() {
        return Collections.unmodifiableCollection(this.cloudServices.values());
    }

    @Override
    public Collection<Integer> getReservedTaskIds(@NotNull String task) {
        Preconditions.checkNotNull(task);

        Collection<Integer> taskIdList = new ArrayList<>();

        for (ServiceInfoSnapshot serviceInfoSnapshot : this.globalServiceInfoSnapshots.values()) {
            if (serviceInfoSnapshot.getServiceId().getTaskName().equalsIgnoreCase(task)) {
                taskIdList.add(serviceInfoSnapshot.getServiceId().getTaskServiceId());
            }
        }

        return taskIdList;
    }

    @Override
    public int getCurrentUsedHeapMemory() {
        int value = 0;

        for (ICloudService cloudService : this.cloudServices.values()) {
            if (cloudService.getLifeCycle() == ServiceLifeCycle.RUNNING) {
                value += cloudService.getConfiguredMaxHeapMemory();
            }
        }

        return value;
    }

    @Override
    public int getCurrentReservedMemory() {
        int value = 0;

        for (ICloudService cloudService : this.cloudServices.values()) {
            value += cloudService.getConfiguredMaxHeapMemory();
        }

        return value;
    }

    protected int checkAndReplacePort(int port) {
        Collection<Integer> usedPorts = new HashSet<>();

        for (ICloudService cloudService : this.cloudServices.values()) {
            usedPorts.add(cloudService.getServiceConfiguration().getPort());
        }

        boolean portBindRetry = false;
        while (usedPorts.contains(port) || (portBindRetry = !PortValidator.checkPort(port))) {
            int oldPort = port++;

            if (portBindRetry) {
                CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-port-bind-retry-message")
                        .replace("%port%", String.valueOf(oldPort))
                        .replace("%next_port%", String.valueOf(port)));

                portBindRetry = false;
            }

        }

        return port;
    }

    private int checkAndReplaceTaskId(ServiceId serviceId) {
        int taskId = serviceId.getTaskServiceId();
        if (taskId <= 0) {
            taskId = 1;
        }

        Collection<Integer> taskIdList = this.getReservedTaskIds(serviceId.getTaskName());
        while (taskIdList.contains(taskId)) {
            taskId++;
        }

        return taskId;
    }

    @NotNull
    public File getTempDirectory() {
        return this.tempDirectory;
    }

    @NotNull
    public File getPersistenceServicesDirectory() {
        return this.persistenceServicesDirectory;
    }

    public @NotNull Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots() {
        return this.globalServiceInfoSnapshots;
    }

    public @NotNull Map<UUID, ICloudService> getCloudServices() {
        return this.cloudServices;
    }

    @NotNull
    @Override
    public Collection<ICloudServiceFactory> getCloudServiceFactories() {
        return this.cloudServiceFactories.values();
    }

    @Override
    public @NotNull Optional<ICloudServiceFactory> getCloudServiceFactory(String runtime) {
        return runtime == null ? Optional.empty() : Optional.ofNullable(this.cloudServiceFactories.get(runtime));
    }
}
