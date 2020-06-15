package de.dytanic.cloudnet.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.event.service.CloudServiceCreateEvent;
import de.dytanic.cloudnet.util.PortValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class DefaultCloudServiceManager implements ICloudServiceManager {

    private static final ICloudServiceFactory DEFAULT_FACTORY = new DefaultCloudServiceFactory(JVMCloudService.RUNTIME, JVMCloudService::new);
    private final File
            tempDirectory = new File(System.getProperty("cloudnet.tempDir.services", "temp/services")),
            persistenceServicesDirectory = new File(System.getProperty("cloudnet.persistable.services.path", "local/services"));
    private final Map<UUID, ServiceInfoSnapshot> globalServiceInfoSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, ICloudService> cloudServices = new ConcurrentHashMap<>();
    private final Map<String, ICloudServiceFactory> cloudServiceFactories = new ConcurrentHashMap<>();

    @Override
    public ICloudService runTask(@NotNull ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.runTask(serviceTask, this.findTaskId(serviceTask.getName()));
    }

    @Override
    public ICloudService runTask(@NotNull ServiceTask serviceTask, int taskId) {
        Preconditions.checkNotNull(serviceTask);

        return this.runTask(
                serviceTask.getName(),
                serviceTask.getRuntime(),
                taskId,
                serviceTask.isAutoDeleteOnStop(),
                serviceTask.isStaticServices(),
                new ArrayList<>(serviceTask.getIncludes()),
                new ArrayList<>(serviceTask.getTemplates()),
                new ArrayList<>(serviceTask.getDeployments()),
                new ArrayList<>(serviceTask.getGroups()),
                serviceTask.getDeletedFilesAfterStop(),
                new ProcessConfiguration(
                        serviceTask.getProcessConfiguration().getEnvironment(),
                        serviceTask.getProcessConfiguration().getMaxHeapMemorySize(),
                        new ArrayList<>(serviceTask.getProcessConfiguration().getJvmOptions())
                ),
                serviceTask.getProperties(),
                serviceTask.getStartPort()
        );
    }

    @Override
    public ICloudService runTask(@NotNull ServiceConfiguration serviceConfiguration) {
        CloudServiceCreateEvent cloudServiceCreateEvent = new CloudServiceCreateEvent(serviceConfiguration);
        CloudNetDriver.getInstance().getEventManager().callEvent(cloudServiceCreateEvent);

        if (cloudServiceCreateEvent.isCancelled()) {
            return null;
        }

        serviceConfiguration.setPort(this.checkAndReplacePort(serviceConfiguration.getPort()));

        ICloudService cloudService = this.getCloudServiceFactory(serviceConfiguration.getRuntime())
                .map(factory -> factory.createCloudService(this, serviceConfiguration))
                .orElseGet(() -> DEFAULT_FACTORY.createCloudService(this, serviceConfiguration));

        if (cloudService != null) {
            this.cloudServices.put(cloudService.getServiceId().getUniqueId(), cloudService);
            this.globalServiceInfoSnapshots.put(cloudService.getServiceId().getUniqueId(), cloudService.getServiceInfoSnapshot());

            CloudNet.getInstance().getNetworkClient()
                    .sendPacket(new PacketClientServerServiceInfoPublisher(cloudService.getServiceInfoSnapshot(), PacketClientServerServiceInfoPublisher.PublisherType.REGISTER)); // TODO: is this really necessary?
            CloudNet.getInstance().getNetworkServer()
                    .sendPacket(new PacketClientServerServiceInfoPublisher(cloudService.getServiceInfoSnapshot(), PacketClientServerServiceInfoPublisher.PublisherType.REGISTER));

            CloudNet.getInstance().publishNetworkClusterNodeInfoSnapshotUpdate();
        }

        return cloudService;
    }

    @Override
    public ICloudService runTask(
            String name,
            String runtime,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            Collection<String> deletedFilesAfterStop,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    ) {
        Preconditions.checkNotNull(name);

        return this.runTask(name, runtime, this.findTaskId(name), autoDeleteOnStop, staticService, includes, templates, deployments, groups, deletedFilesAfterStop, processConfiguration, properties, port);
    }

    private int findTaskId(String taskName) {
        int taskId = 1;

        Collection<Integer> taskIdList = this.getReservedTaskIds(taskName);

        while (taskIdList.contains(taskId)) {
            taskId++;
        }

        return taskId;
    }

    @Override
    public ICloudService runTask(
            String name,
            String runtime,
            int taskId,
            boolean autoDeleteOnStop,
            boolean staticService,
            Collection<ServiceRemoteInclusion> includes,
            Collection<ServiceTemplate> templates,
            Collection<ServiceDeployment> deployments,
            Collection<String> groups,
            Collection<String> deletedFilesAfterStop,
            ProcessConfiguration processConfiguration,
            JsonDocument properties,
            Integer port
    ) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        if (this.getReservedTaskIds(name).contains(taskId)) {
            return null;
        }

        List<ServiceTemplate> allTemplates = new ArrayList<>();

        for (GroupConfiguration groupConfiguration : CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations()) {
            String groupName = groupConfiguration.getName();

            if (!groups.contains(groupName) && groupConfiguration.getTargetEnvironments().contains(processConfiguration.getEnvironment())) {
                groups.add(groupName);
            }

            if (groups.contains(groupName)) {
                includes.addAll(groupConfiguration.getIncludes());
                allTemplates.addAll(groupConfiguration.getTemplates());
                deployments.addAll(groupConfiguration.getDeployments());

                processConfiguration.getJvmOptions().addAll(groupConfiguration.getJvmOptions());

                if (properties != null) {
                    properties.append(groupConfiguration.getProperties());
                }
            }
        }

        // adding the task templates after the group templates for them to have a higher priority
        allTemplates.addAll(templates);

        ServiceConfiguration serviceConfiguration = new ServiceConfiguration(
                new ServiceId(
                        UUID.randomUUID(),
                        CloudNet.getInstance().getConfig().getIdentity().getUniqueId(),
                        name,
                        taskId,
                        processConfiguration.getEnvironment()
                ),
                runtime,
                autoDeleteOnStop,
                staticService,
                groups.toArray(new String[0]),
                includes.toArray(new ServiceRemoteInclusion[0]),
                allTemplates.toArray(new ServiceTemplate[0]),
                deployments.toArray(new ServiceDeployment[0]),
                deletedFilesAfterStop != null ? deletedFilesAfterStop.toArray(new String[0]) : new String[0],
                processConfiguration,
                properties,
                port
        );

        return this.runTask(serviceConfiguration);
    }

    @Override
    public void startAllCloudServices() {
        this.executeForAllServices(cloudService -> {
            try {
                cloudService.start();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void stopAllCloudServices() {
        this.executeForAllServices(cloudService -> {
            try {
                cloudService.stop();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void deleteAllCloudServices() {
        this.executeForAllServices(cloudService -> {
            try {
                cloudService.delete();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private void executeForAllServices(Consumer<ICloudService> consumer) {
        if (this.cloudServices.isEmpty()) {
            return;
        }
        Collection<ICloudService> cloudServices = new ArrayList<>(this.cloudServices.values());

        CountDownLatch countDownLatch = new CountDownLatch(cloudServices.size());
        ExecutorService executorService = Executors.newFixedThreadPool((cloudServices.size() / 2) + 1);

        for (ICloudService cloudService : this.cloudServices.values()) {
            executorService.execute(() -> {
                consumer.accept(cloudService);
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

    @Nullable
    @Override
    public ServiceInfoSnapshot getServiceInfoSnapshot(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);

        return this.globalServiceInfoSnapshots.get(uniqueId);
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot getServiceInfoSnapshot(@NotNull Predicate<ServiceInfoSnapshot> predicate) {
        Preconditions.checkNotNull(predicate);

        return this.globalServiceInfoSnapshots.values().stream().filter(predicate).findFirst().orElse(null);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(@NotNull String taskName) {
        Preconditions.checkNotNull(taskName);

        return this.getServiceInfoSnapshots(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName().equalsIgnoreCase(taskName));
    }

    @Override
    public Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(@NotNull ServiceEnvironmentType environment) {
        Preconditions.checkNotNull(environment);

        return this.getServiceInfoSnapshots(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getEnvironment() == environment);
    }

    @Override
    public Collection<ServiceInfoSnapshot> getServiceInfoSnapshots(@NotNull Predicate<ServiceInfoSnapshot> predicate) {
        Preconditions.checkNotNull(predicate);

        return this.globalServiceInfoSnapshots.values().stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public Collection<ServiceInfoSnapshot> getServiceInfoSnapshots() {
        return this.globalServiceInfoSnapshots.values();
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

    private int checkAndReplacePort(int port) {
        Collection<Integer> ports = this.cloudServices.values().stream().map(iCloudService -> iCloudService.getServiceConfiguration().getPort()).collect(Collectors.toList());

        while (ports.contains(port)) {
            port++;
        }

        while (!PortValidator.checkPort(port)) {
            CloudNetDriver.getInstance().getLogger().extended(LanguageManager.getMessage("cloud-service-port-bind-retry-message")
                    .replace("%port%", String.valueOf(port))
                    .replace("%next_port%", String.valueOf(++port)));
        }

        return port;
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
