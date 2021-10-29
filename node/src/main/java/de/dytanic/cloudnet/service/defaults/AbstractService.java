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
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.io.HttpConnectionProvider;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.config.IConfiguration;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.def.NetworkConstants;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.event.service.CloudServiceCreateEvent;
import de.dytanic.cloudnet.event.service.CloudServiceDeploymentEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostLifecycleEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreLifecycleEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreLoadInclusionEvent;
import de.dytanic.cloudnet.event.service.CloudServiceTemplateLoadEvent;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.IServiceConsoleLogCache;
import de.dytanic.cloudnet.service.ServiceConfigurationPreparer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public abstract class AbstractService implements ICloudService {

  protected static final Logger LOGGER = LogManager.getLogger(AbstractService.class);

  protected static final Path INCLUSION_TEMP_DIR = FileUtils.TEMP_DIR.resolve("inclusions");
  protected static final Path WRAPPER_CONFIG_PATH = Paths.get(".wrapper", "wrapper.json");
  protected static final Collection<String> DEFAULT_DEPLOYMENT_EXCLUSIONS = Arrays.asList("wrapper.jar", ".wrapper/");

  protected final IEventManager eventManager;

  protected final String connectionKey;
  protected final Path serviceDirectory;
  protected final CloudNet nodeInstance;
  protected final ICloudServiceManager cloudServiceManager;
  protected final ServiceConfiguration serviceConfiguration;
  protected final ServiceConfigurationPreparer serviceConfigurationPreparer;

  protected final Lock lifecycleLock = new ReentrantLock(true);

  protected final Queue<ServiceTemplate> waitingTemplates = new ConcurrentLinkedQueue<>();
  protected final Queue<ServiceDeployment> waitingDeployments = new ConcurrentLinkedQueue<>();
  protected final Queue<ServiceRemoteInclusion> waitingRemoteInclusions = new ConcurrentLinkedQueue<>();

  protected IServiceConsoleLogCache logCache;
  protected volatile INetworkChannel networkChannel;

  protected volatile ServiceInfoSnapshot lastServiceInfo;
  protected volatile ServiceInfoSnapshot currentServiceInfo;

  protected AbstractService(
    @NotNull ServiceConfiguration configuration,
    @NotNull ICloudServiceManager manager,
    @NotNull IEventManager eventManager,
    @NotNull CloudNet nodeInstance,
    @NotNull ServiceConfigurationPreparer serviceConfigurationPreparer
  ) {
    this.eventManager = eventManager;
    this.nodeInstance = nodeInstance;
    this.cloudServiceManager = manager;
    this.serviceConfiguration = configuration;
    this.serviceConfigurationPreparer = serviceConfigurationPreparer;

    this.connectionKey = StringUtil.generateRandomString(64);
    this.serviceDirectory = resolveServicePath(configuration.getServiceId(), manager, configuration.isStaticService());

    this.currentServiceInfo = new ServiceInfoSnapshot(
      System.currentTimeMillis(),
      -1,
      new HostAndPort(this.getNodeConfiguration().getHostAddress(), configuration.getPort()),
      new HostAndPort(this.getNodeConfiguration().getConnectHostAddress(), configuration.getPort()),
      ServiceLifeCycle.PREPARED,
      ProcessSnapshot.empty(),
      configuration,
      configuration.getProperties());
    this.pushServiceInfoSnapshotUpdate(ServiceLifeCycle.PREPARED);

    manager.registerLocalService(this);
    nodeInstance.getEventManager().callEvent(new CloudServiceCreateEvent(this));
  }

  protected static @NotNull Path resolveServicePath(
    @NotNull ServiceId serviceId,
    @NotNull ICloudServiceManager manager,
    boolean staticService
  ) {
    return staticService
      ? manager.getPersistentServicesDirectoryPath().resolve(serviceId.getName())
      : manager.getTempDirectoryPath().resolve(String.format("%s_%s", serviceId.getName(), serviceId.getUniqueId()));
  }

  @Override
  public @NotNull ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.currentServiceInfo;
  }

  @Override
  public boolean isValid() {
    return this.currentServiceInfo.getLifeCycle() != ServiceLifeCycle.DELETED;
  }

  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    // check if the service is able to serve the request
    if (this.networkChannel != null) {
      ChannelMessage response = ChannelMessage.builder()
        .targetService(this.getServiceId().getName())
        .message("request_update_service_information")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .build()
        .sendSingleQuery();
      return response == null ? this.currentServiceInfo : response.getContent().readObject(ServiceInfoSnapshot.class);
    } else {
      return this.currentServiceInfo;
    }
  }

  @Override
  public void addServiceTemplate(@NotNull ServiceTemplate serviceTemplate) {
    this.waitingTemplates.add(Preconditions.checkNotNull(serviceTemplate, "template"));
  }

  @Override
  public void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
    this.waitingRemoteInclusions.add(Preconditions.checkNotNull(serviceRemoteInclusion, "remoteInclusion"));
  }

  @Override
  public void addServiceDeployment(@NotNull ServiceDeployment serviceDeployment) {
    this.waitingDeployments.add(Preconditions.checkNotNull(serviceDeployment, "deployment"));
  }

  @Override
  public @NotNull IServiceConsoleLogCache getServiceConsoleLogCache() {
    return this.logCache;
  }

  @Override
  public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
    try {
      // prevent multiple service updates at the same time
      this.lifecycleLock.lock();
      // prevent changing the lifecycle to an incompatible lifecycle
      if (!this.getLifeCycle().canChangeTo(lifeCycle)) {
        return;
      }
      // select the appropriate method for the lifecycle
      switch (lifeCycle) {
        case DELETED: {
          if (this.preLifecycleChange(ServiceLifeCycle.DELETED)) {
            this.doDelete();
            // update the current service info
            this.pushServiceInfoSnapshotUpdate(ServiceLifeCycle.DELETED);
          }
          break;
        }

        case RUNNING: {
          if (this.preLifecycleChange(ServiceLifeCycle.RUNNING)) {
            // check if we can start the process now
            if (this.getLifeCycle() == ServiceLifeCycle.PREPARED && this.canStartNow()) {
              this.prepareService();
              this.startProcess();
              // update the current service info
              this.pushServiceInfoSnapshotUpdate(ServiceLifeCycle.RUNNING);
            }
          }
          break;
        }

        case STOPPED: {
          if (this.preLifecycleChange(ServiceLifeCycle.STOPPED)) {
            // check if we should delete the service when stopping
            if (this.getServiceConfiguration().isAutoDeleteOnStop()) {
              this.doDelete();
              // update the current service info
              this.pushServiceInfoSnapshotUpdate(ServiceLifeCycle.DELETED);
            } else if (this.getLifeCycle() == ServiceLifeCycle.RUNNING) {
              this.stopProcess();
              this.doRemoveFilesAfterStop();
              // reset the service lifecycle to prepared
              this.pushServiceInfoSnapshotUpdate(ServiceLifeCycle.PREPARED);
            }
          }
          break;
        }

        case PREPARED:
          break; // cannot be set - just ignore

        default:
          throw new IllegalStateException("Unhandled ServiceLifeCycle: " + lifeCycle);
      }
    } finally {
      this.lifecycleLock.unlock();
    }
  }

  @Override
  public void restart() {
    this.stop();
    this.start();
  }

  @Override
  public void includeWaitingServiceTemplates() {
    this.includeWaitingServiceTemplates(false);
  }

  @Override
  public void includeWaitingServiceInclusions() {
    ServiceRemoteInclusion inclusion;
    while ((inclusion = this.waitingRemoteInclusions.poll()) != null) {
      // prepare the connection from which we load the inclusion
      HttpURLConnection con = HttpConnectionProvider.provideConnection(inclusion.getUrl());
      con.setUseCaches(false);
      // put the given http headers
      if (inclusion.getProperties().contains("httpHeaders")) {
        JsonDocument headers = inclusion.getProperties().getDocument("httpHeaders");
        for (String key : headers.keys()) {
          con.setRequestProperty(key, headers.get(key).toString());
        }
      }
      // check if we should load the inclusion
      if (!this.eventManager.callEvent(new CloudServicePreLoadInclusionEvent(this, inclusion, con)).isCancelled()) {
        // get a target path based on the download url
        Path destination = INCLUSION_TEMP_DIR.resolve(
          Base64.getEncoder().encodeToString(inclusion.getUrl().getBytes(StandardCharsets.UTF_8)).replace('/', '_'));
        // download the file from the given url to the temp path if it does not exist
        if (Files.notExists(destination)) {
          try {
            con.connect();
            // we only support success codes for downloading the file
            try (InputStream in = con.getInputStream()) {
              Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
          } catch (IOException exception) {
            LOGGER.severe("Unable to download inclusion from %s to %s", exception, inclusion.getUrl(), destination);
            continue;
          }
        }
        // resolve the desired output path
        Path target = this.serviceDirectory.resolve(inclusion.getDestination());
        FileUtils.ensureChild(this.serviceDirectory, target);
        // copy the file to the desired output path
        FileUtils.copy(destination, target);
      }
    }
  }

  @Override
  public void deployResources(boolean removeDeployments) {
    if (removeDeployments) {
      // remove all deployments while execute the deployments
      ServiceDeployment deployment;
      while ((deployment = this.waitingDeployments.poll()) != null) {
        this.executeDeployment(deployment);
      }
    } else {
      // just execute all deployments
      for (ServiceDeployment deployment : this.waitingDeployments) {
        this.executeDeployment(deployment);
      }
    }
  }

  @Override
  public void doDelete() {
    // stop the process if it's running
    if (this.currentServiceInfo.getLifeCycle() == ServiceLifeCycle.RUNNING || this.isAlive()) {
      this.stopProcess();
    }
    // execute all deployments which are still waiting - delete all requested files before that
    this.doRemoveFilesAfterStop();
    this.removeAndExecuteDeployments();
    // remove the current directory if the service is not static
    if (!this.getServiceConfiguration().isStaticService()) {
      FileUtils.delete(this.serviceDirectory);
    }
  }

  @Override
  public @UnmodifiableView @NotNull Queue<ServiceRemoteInclusion> getWaitingIncludes() {
    return this.waitingRemoteInclusions;
  }

  @Override
  public @UnmodifiableView @NotNull Queue<ServiceTemplate> getWaitingTemplates() {
    return this.waitingTemplates;
  }

  @Override
  public @UnmodifiableView @NotNull Queue<ServiceDeployment> getWaitingDeployments() {
    return this.waitingDeployments;
  }

  @Override
  public @NotNull ServiceLifeCycle getLifeCycle() {
    return this.currentServiceInfo.getLifeCycle();
  }

  @Override
  public @NotNull ICloudServiceManager getCloudServiceManager() {
    return this.cloudServiceManager;
  }

  @Override
  public @NotNull ServiceConfiguration getServiceConfiguration() {
    return this.currentServiceInfo.getConfiguration();
  }

  @Override
  public @NotNull ServiceId getServiceId() {
    return this.currentServiceInfo.getServiceId();
  }

  @Override
  public @NotNull String getConnectionKey() {
    return this.connectionKey;
  }

  @Override
  public @NotNull Path getDirectory() {
    return this.serviceDirectory;
  }

  @Override
  public @Nullable INetworkChannel getNetworkChannel() {
    return this.networkChannel;
  }

  @Override
  public void setNetworkChannel(@Nullable INetworkChannel channel) {
    Preconditions.checkArgument(this.networkChannel == null || channel == null);
    // close the channel if the new channel is null
    if (this.networkChannel != null) {
      this.networkChannel.close();
      this.currentServiceInfo.setConnectedTime(0);
    } else {
      this.currentServiceInfo.setConnectedTime(System.currentTimeMillis());
    }
    // set the new channel
    this.networkChannel = channel;
  }

  @Override
  public @NotNull ServiceInfoSnapshot getLastServiceInfoSnapshot() {
    return this.lastServiceInfo;
  }

  @Override
  public void publishServiceInfoSnapshot() {
    ChannelMessage.builder()
      .targetAll()
      .message("update_service_info")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(this.currentServiceInfo))
      .build()
      .send();
  }

  @Override
  public void updateServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    this.currentServiceInfo = Preconditions.checkNotNull(serviceInfoSnapshot, "serviceInfoSnapshot");
  }

  @Override
  public Queue<String> getCachedLogMessages() {
    return this.getServiceConsoleLogCache().getCachedLogMessages();
  }

  protected @NotNull IConfiguration getNodeConfiguration() {
    return this.nodeInstance.getConfig();
  }

  protected void includeWaitingServiceTemplates(boolean automaticRequest) {
    this.waitingTemplates.stream()
      .filter(template -> {
        // always allow manual requests & non-static service copies
        if (!automaticRequest || !this.getServiceConfiguration().isStaticService()) {
          return true;
        }
        // only allow this template to be copied if explicitly defined
        return template.shouldAlwaysCopyToStaticServices();
      })
      .sorted()
      .forEachOrdered(template -> {
        // remove the entry
        this.waitingTemplates.remove(template);
        // check if we should load the template
        TemplateStorage storage = template.storage().getWrappedStorage();
        if (!this.eventManager.callEvent(new CloudServiceTemplateLoadEvent(this, storage, template)).isCancelled()) {
          // the event is not cancelled - copy the template
          storage.copy(template, this.serviceDirectory);
        }
      });
  }

  protected void executeDeployment(@NotNull ServiceDeployment deployment) {
    // check if we should execute the deployment
    TemplateStorage storage = deployment.getTemplate().storage().getWrappedStorage();
    if (!this.eventManager.callEvent(new CloudServiceDeploymentEvent(this, storage, deployment)).isCancelled()) {
      // execute the deployment
      storage.deployDirectory(this.serviceDirectory, deployment.getTemplate(), path -> {
        // normalize the name of the path
        String fileName = Files.isDirectory(path)
          ? path.getFileName().toString() + '/'
          : path.getFileName().toString();
        // check if the file is ignored
        return deployment.getExcludes().stream().noneMatch(pattern -> fileName.matches(pattern.replace("*", "(.*)")))
          && !DEFAULT_DEPLOYMENT_EXCLUSIONS.contains(fileName);
      });
    }
  }

  protected void doRemoveFilesAfterStop() {
    for (String file : this.serviceConfiguration.getDeletedFilesAfterStop()) {
      FileUtils.delete(this.serviceDirectory.resolve(file));
    }
  }

  protected boolean preLifecycleChange(@NotNull ServiceLifeCycle targetLifecycle) {
    return !this.eventManager.callEvent(new CloudServicePreLifecycleEvent(this, targetLifecycle)).isCancelled();
  }

  protected void pushServiceInfoSnapshotUpdate(@NotNull ServiceLifeCycle lifeCycle) {
    // save the current service info
    this.lastServiceInfo = this.currentServiceInfo;
    // update the current info
    this.currentServiceInfo = new ServiceInfoSnapshot(
      this.lastServiceInfo.getCreationTime(),
      this.networkChannel == null ? -1 : this.lastServiceInfo.getConnectedTime(),
      this.lastServiceInfo.getAddress(),
      this.lastServiceInfo.getConnectAddress(),
      Preconditions.checkNotNull(lifeCycle, "lifecycle"),
      this.isAlive() ? this.lastServiceInfo.getProcessSnapshot() : ProcessSnapshot.empty(),
      this.lastServiceInfo.getConfiguration(),
      this.lastServiceInfo.getProperties());
    // remove the service in the local manager if the service was deleted
    if (lifeCycle == ServiceLifeCycle.DELETED) {
      this.cloudServiceManager.unregisterLocalService(this);
    }
    // call the lifecycle change event
    this.eventManager.callEvent(new CloudServicePostLifecycleEvent(this, lifeCycle));
    // publish the change to all services and nodes
    ChannelMessage.builder()
      .targetAll()
      .message("update_service_lifecycle")
      .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
      .buffer(DataBuf.empty().writeObject(this.lastServiceInfo.getLifeCycle()).writeObject(this.currentServiceInfo))
      .build()
      .send();
  }

  protected boolean canStartNow() {
    // check jvm heap size
    if (this.cloudServiceManager.getCurrentUsedHeapMemory()
      + this.getServiceConfiguration().getProcessConfig().getMaxHeapMemorySize()
      >= this.getNodeConfiguration().getMaxMemory()) {
      // schedule a retry
      if (this.getNodeConfiguration().isRunBlockedServiceStartTryLaterAutomatic()) {
        CloudNet.getInstance().getMainThread().runTask(this::start);
      } else {
        LOGGER.info(I18n.trans("cloud-service-manager-max-memory-error"));
      }
      // no starting now
      return false;
    }
    // check for cpu usage
    if (CPUUsageResolver.getSystemCPUUsage() >= this.getNodeConfiguration().getMaxCPUUsageToStartServices()) {
      // schedule a retry
      if (this.getNodeConfiguration().isRunBlockedServiceStartTryLaterAutomatic()) {
        CloudNet.getInstance().getMainThread().runTask(this::start);
      } else {
        LOGGER.info(I18n.trans("cloud-service-manager-cpu-usage-to-high-error"));
      }
      // no starting now
      return false;
    }
    // ok to start now
    return true;
  }

  protected void prepareService() {
    // initialize the service directory
    boolean firstStartup = Files.notExists(this.serviceDirectory);
    FileUtils.createDirectory(this.serviceDirectory);
    // write the configuration file for the service
    HostAndPort[] listeners = this.getNodeConfiguration().getIdentity().getListeners();
    JsonDocument.newDocument()
      .append("connectionKey", this.getConnectionKey())
      .append("serviceInfoSnapshot", this.currentServiceInfo)
      .append("serviceConfiguration", this.getServiceConfiguration())
      .append("sslConfiguration", this.getNodeConfiguration().getServerSslConfig())
      .append("targetListener", listeners[ThreadLocalRandom.current().nextInt(listeners.length)])
      .write(this.serviceDirectory.resolve(WRAPPER_CONFIG_PATH));
    // load the ssl configuration if enabled
    SSLConfiguration sslConfiguration = this.getNodeConfiguration().getServerSslConfig();
    if (sslConfiguration.isEnabled()) {
      this.copySslConfiguration(sslConfiguration);
    }
    // add all components
    this.waitingTemplates.addAll(this.serviceConfiguration.getTemplates());
    this.waitingDeployments.addAll(this.serviceConfiguration.getDeployments());
    this.waitingRemoteInclusions.addAll(this.serviceConfiguration.getIncludes());
    // load the inclusions
    this.includeWaitingServiceInclusions();
    // check if we should load the templates of the service
    this.includeWaitingServiceTemplates(!firstStartup);
    // update the service configuration
    this.serviceConfigurationPreparer.configure(this.nodeInstance, this);
  }

  protected void copySslConfiguration(@NotNull SSLConfiguration configuration) {
    Path wrapperDir = this.serviceDirectory.resolve(".wrapper");
    // copy the certificate if available
    if (configuration.getCertificatePath() != null && Files.exists(configuration.getCertificatePath())) {
      FileUtils.copy(configuration.getCertificatePath(), wrapperDir.resolve("certificate"));
    }
    // copy the private key if available
    if (configuration.getPrivateKeyPath() != null && Files.exists(configuration.getPrivateKeyPath())) {
      FileUtils.copy(configuration.getPrivateKeyPath(), wrapperDir.resolve("privateKey"));
    }
    // copy the trust certificate if available
    if (configuration.getTrustCertificatePath() != null && Files.exists(configuration.getTrustCertificatePath())) {
      FileUtils.copy(configuration.getTrustCertificatePath(), wrapperDir.resolve("trustCertificate"));
    }
  }

  protected abstract void startProcess();

  protected abstract void stopProcess();
}
