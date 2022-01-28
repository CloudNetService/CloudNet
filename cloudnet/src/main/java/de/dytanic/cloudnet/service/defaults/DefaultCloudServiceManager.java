/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.service.defaults;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.NodeServer;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.function.ThrowableConsumer;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceDisconnectNetworkEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceRegisterEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStartEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceStopEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUnregisterEvent;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.event.service.CloudServiceCreateEvent;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceFactory;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.handler.CloudServiceHandler;
import de.dytanic.cloudnet.service.handler.DefaultCloudServiceHandler;
import de.dytanic.cloudnet.util.PortValidator;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultCloudServiceManager implements ICloudServiceManager {

  private static final CloudServiceHandler HANDLER = DefaultCloudServiceHandler.INSTANCE;
  private static final ICloudServiceFactory DEFAULT_FACTORY = new DefaultCloudServiceFactory(JVMCloudService.RUNTIME,
    (manager, configuration) -> new JVMCloudService(manager, configuration, HANDLER));

  private final Path tempDirectory = Paths.get(System.getProperty("cloudnet.tempDir.services", "temp/services"));
  private final Path persistenceServicesDirectory = Paths
    .get(System.getProperty("cloudnet.persistable.services.path", "local/services"));

  private final Lock globalServicesUpdateLock = new ReentrantLock();
  private final Map<UUID, ServiceInfoSnapshot> globalServiceInfoSnapshots = new ConcurrentHashMap<>();

  private final Map<UUID, ICloudService> cloudServices = new ConcurrentHashMap<>();
  private final Map<String, ICloudServiceFactory> cloudServiceFactories = new ConcurrentHashMap<>();

  public DefaultCloudServiceManager(@NotNull ScheduledExecutorService service) {
    service.scheduleAtFixedRate(() -> {
      try {
        this.stopDeadServices();
        this.updateServiceLogs();
      } catch (Throwable throwable) {
        CloudNet.getInstance().getLogger().error("Exception while ticking the cloud service manager", throwable);
      }
    }, 1, 1, TimeUnit.SECONDS);
  }

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
  public ITask<ICloudService> createCloudService(@NotNull ServiceConfiguration serviceConfiguration,
    @Nullable Long timeoutMillis) {
    if (CloudNet.getInstance().isMainThread()) {
      return CompletedTask.create(this.createCloudServiceSync(serviceConfiguration, timeoutMillis));
    } else {
      return CloudNet.getInstance().runTask(() -> this.createCloudServiceSync(serviceConfiguration, timeoutMillis));
    }
  }

  private ICloudService createCloudServiceSync(@NotNull ServiceConfiguration serviceConfiguration,
    @Nullable Long timeoutMillis) {
    this.prepareServiceConfiguration(serviceConfiguration);

    CloudServiceCreateEvent event = new CloudServiceCreateEvent(serviceConfiguration);
    CloudNetDriver.getInstance().getEventManager().callEvent(event);

    if (event.isCancelled()) {
      return null;
    }

    ICloudService cloudService = this.getCloudServiceFactory(serviceConfiguration.getRuntime())
      .map(factory -> factory.createCloudService(this, serviceConfiguration))
      .orElseGet(() -> DEFAULT_FACTORY.createCloudService(this, serviceConfiguration));

    if (cloudService != null) {
      cloudService.init();

      if (timeoutMillis == null || timeoutMillis >= System.currentTimeMillis()) {
        this.cloudServices.put(cloudService.getServiceId().getUniqueId(), cloudService);
        this.globalServiceInfoSnapshots
          .put(cloudService.getServiceId().getUniqueId(), cloudService.getServiceInfoSnapshot());

        CloudNet.getInstance().sendAll(new PacketClientServerServiceInfoPublisher(cloudService.getServiceInfoSnapshot(),
          PacketClientServerServiceInfoPublisher.PublisherType.REGISTER));
        CloudNet.getInstance().publishNetworkClusterNodeInfoSnapshotUpdate();
      } else {
        cloudService.delete(false);
        return null;
      }
    }

    return cloudService;
  }

  private void prepareServiceConfiguration(ServiceConfiguration configuration) {
    configuration.setPort(this.checkAndReplacePort(configuration.getPort()));

    Collection<String> groups = new ArrayList<>(Arrays.asList(configuration.getGroups()));

    Collection<ServiceTemplate> templates = new ArrayList<>();
    Collection<ServiceDeployment> deployments = new ArrayList<>();
    Collection<ServiceRemoteInclusion> inclusions = new ArrayList<>();

    for (GroupConfiguration groupConfiguration : CloudNet.getInstance().getGroupConfigurationProvider()
      .getGroupConfigurations()) {
      String groupName = groupConfiguration.getName();

      if (!groups.contains(groupName) && groupConfiguration.getTargetEnvironments()
        .contains(configuration.getProcessConfig().getEnvironment())) {
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
    if (!this.cloudServices.isEmpty()) {
      Collection<ICloudService> cloudServices = new ArrayList<>(this.cloudServices.values());
      ExecutorService executorService = Executors.newFixedThreadPool((cloudServices.size() / 2) + 1);

      for (ICloudService cloudService : cloudServices) {
        executorService.execute(() -> {
          try {
            consumer.accept(cloudService);
          } catch (Exception exception) {
            exception.printStackTrace();
          }
        });
      }

      try {
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);
      } catch (InterruptedException exception) {
        exception.printStackTrace();
      }
    }
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

    return this.cloudServices.values().stream()
      .filter(service -> service.getServiceId().getTaskName().equalsIgnoreCase(taskName))
      .collect(Collectors.toList());
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

  private int checkAndReplacePort(int port) {
    Collection<Integer> usedPorts = new HashSet<>();

    for (ICloudService cloudService : this.cloudServices.values()) {
      usedPorts.add(cloudService.getServiceConfiguration().getPort());
    }

    boolean portBindRetry = false;
    while (usedPorts.contains(port) || (portBindRetry = !PortValidator.checkPort(port))) {
      int oldPort = port++;

      if (portBindRetry) {
        CloudNetDriver.getInstance().getLogger()
          .extended(LanguageManager.getMessage("cloud-service-port-bind-retry-message")
            .replace("%port%", String.valueOf(oldPort))
            .replace("%next_port%", String.valueOf(port)));

        portBindRetry = false;
      }

    }

    return port;
  }

  @NotNull
  @Override
  public File getTempDirectory() {
    return this.tempDirectory.toFile();
  }

  @NotNull
  @Override
  public Path getTempDirectoryPath() {
    return this.tempDirectory;
  }

  @NotNull
  @Override
  public File getPersistenceServicesDirectory() {
    return this.persistenceServicesDirectory.toFile();
  }

  @NotNull
  @Override
  public Path getPersistentServicesDirectoryPath() {
    return this.persistenceServicesDirectory;
  }

  @Override
  public @NotNull Map<UUID, ServiceInfoSnapshot> getGlobalServiceInfoSnapshots() {
    return this.globalServiceInfoSnapshots;
  }

  @Override
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

  @Override
  public void addCloudServiceFactory(@NotNull ICloudServiceFactory factory) {
    this.cloudServiceFactories.putIfAbsent(factory.getRuntime(), factory);
  }

  @Override
  public void removeCloudServiceFactory(@NotNull ICloudServiceFactory factory) {
    this.removeCloudServiceFactory(factory.getRuntime());
  }

  @Override
  public void removeCloudServiceFactory(@NotNull String runtime) {
    this.cloudServiceFactories.remove(runtime);
  }

  @Override
  public boolean handleServiceUpdate(PacketClientServerServiceInfoPublisher.@NotNull PublisherType type,
    @NotNull ServiceInfoSnapshot snapshot) {
    try {
      this.globalServicesUpdateLock.lock();
      return this.doServiceUpdate(type, snapshot);
    } finally {
      this.globalServicesUpdateLock.unlock();
    }
  }

  @ApiStatus.Internal
  public void prepareServiceConfiguration(NodeServer server, ServiceConfiguration configuration) {
    Preconditions.checkArgument(CloudNet.getInstance().isMainThread(), "Async service pre-prepare");

    configuration.getServiceId().setNodeUniqueId(server.getNodeInfo().getUniqueId());
    configuration.getServiceId().setTaskServiceId(this.checkAndReplaceTaskId(configuration.getServiceId()));
  }

  private boolean doServiceUpdate(PacketClientServerServiceInfoPublisher.PublisherType type,
    ServiceInfoSnapshot snapshot) {
    if (this.globalServiceInfoSnapshots.containsKey(snapshot.getServiceId().getUniqueId())) {
      switch (type) {
        case STARTED:
          CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceStartEvent(snapshot));
          break;
        case UPDATE:
          CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceInfoUpdateEvent(snapshot));
          break;
        case CONNECTED:
          CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceConnectNetworkEvent(snapshot));
          break;
        case DISCONNECTED:
          CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceDisconnectNetworkEvent(snapshot));
          break;
        case STOPPED:
          CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceStopEvent(snapshot));
          break;
        case UNREGISTER:
          this.globalServiceInfoSnapshots.remove(snapshot.getServiceId().getUniqueId());
          CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceUnregisterEvent(snapshot));
          return true;
        default:
          return false;
      }

      this.globalServiceInfoSnapshots.put(snapshot.getServiceId().getUniqueId(), snapshot);
      return true;
    } else if (type == PacketClientServerServiceInfoPublisher.PublisherType.REGISTER) {
      this.globalServiceInfoSnapshots.put(snapshot.getServiceId().getUniqueId(), snapshot);
      CloudNetDriver.getInstance().getEventManager().callEvent(new CloudServiceRegisterEvent(snapshot));
      return true;
    } else {
      return false;
    }
  }

  private void stopDeadServices() {
    for (ICloudService cloudService : this.cloudServices.values()) {
      if (!cloudService.isAlive()) {
        cloudService.stop();
      }
    }
  }

  private void updateServiceLogs() {
    for (ICloudService cloudService : this.cloudServices.values()) {
      cloudService.getServiceConsoleLogCache().update();
    }
  }
}
