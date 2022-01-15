/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.service.defaults;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.common.StringUtil;
import eu.cloudnetservice.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.common.unsafe.CPUUsageResolver;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessage;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageTarget;
import eu.cloudnetservice.cloudnet.driver.event.EventManager;
import eu.cloudnetservice.cloudnet.driver.network.HostAndPort;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.network.ssl.SSLConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ProcessSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceConfiguration;
import eu.cloudnetservice.cloudnet.driver.service.ServiceDeployment;
import eu.cloudnetservice.cloudnet.driver.service.ServiceId;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.config.Configuration;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServiceCreateEvent;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServiceDeploymentEvent;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServicePreLifecycleEvent;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServicePreLoadInclusionEvent;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServiceTemplateLoadEvent;
import eu.cloudnetservice.cloudnet.node.service.CloudService;
import eu.cloudnetservice.cloudnet.node.service.CloudServiceManager;
import eu.cloudnetservice.cloudnet.node.service.ServiceConfigurationPreparer;
import eu.cloudnetservice.cloudnet.node.service.ServiceConsoleLogCache;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractService implements CloudService {

  protected static final Logger LOGGER = LogManager.logger(AbstractService.class);

  protected static final Path INCLUSION_TEMP_DIR = FileUtil.TEMP_DIR.resolve("inclusions");
  protected static final Path WRAPPER_CONFIG_PATH = Path.of(".wrapper", "wrapper.json");
  protected static final Collection<String> DEFAULT_DEPLOYMENT_EXCLUSIONS = Arrays.asList("wrapper.jar", ".wrapper/");

  protected final EventManager eventManager;

  protected final String connectionKey;
  protected final Path serviceDirectory;
  protected final CloudNet nodeInstance;
  protected final CloudServiceManager cloudServiceManager;
  protected final ServiceConfiguration serviceConfiguration;
  protected final ServiceConfigurationPreparer serviceConfigurationPreparer;

  protected final Lock lifecycleLock = new ReentrantLock(true);
  protected final Map<ChannelMessageTarget, String> logTargets = new ConcurrentHashMap<>();

  protected final Queue<ServiceTemplate> waitingTemplates = new ConcurrentLinkedQueue<>();
  protected final Queue<ServiceDeployment> waitingDeployments = new ConcurrentLinkedQueue<>();
  protected final Queue<ServiceRemoteInclusion> waitingRemoteInclusions = new ConcurrentLinkedQueue<>();

  protected ServiceConsoleLogCache logCache;
  protected volatile NetworkChannel networkChannel;

  protected volatile ServiceInfoSnapshot lastServiceInfo;
  protected volatile ServiceInfoSnapshot currentServiceInfo;

  protected AbstractService(
    @NonNull ServiceConfiguration configuration,
    @NonNull CloudServiceManager manager,
    @NonNull EventManager eventManager,
    @NonNull CloudNet nodeInstance,
    @NonNull ServiceConfigurationPreparer serviceConfigurationPreparer
  ) {
    this.eventManager = eventManager;
    this.nodeInstance = nodeInstance;
    this.cloudServiceManager = manager;
    this.serviceConfiguration = configuration;
    this.serviceConfigurationPreparer = serviceConfigurationPreparer;

    this.connectionKey = StringUtil.generateRandomString(64);
    this.serviceDirectory = resolveServicePath(configuration.serviceId(), manager, configuration.staticService());

    this.currentServiceInfo = new ServiceInfoSnapshot(
      System.currentTimeMillis(),
      new HostAndPort(this.nodeConfiguration().hostAddress(), configuration.port()),
      new HostAndPort(this.nodeConfiguration().connectHostAddress(), configuration.port()),
      ProcessSnapshot.empty(),
      configuration,
      -1,
      ServiceLifeCycle.PREPARED,
      configuration.properties());
    this.pushServiceInfoSnapshotUpdate(ServiceLifeCycle.PREPARED);

    manager.registerLocalService(this);
    nodeInstance.eventManager().callEvent(new CloudServiceCreateEvent(this));
  }

  protected static @NonNull Path resolveServicePath(
    @NonNull ServiceId serviceId,
    @NonNull CloudServiceManager manager,
    boolean staticService
  ) {
    // validate the service name
    if (!ServiceTask.NAMING_PATTERN.matcher(serviceId.name()).matches()) {
      throw new IllegalArgumentException(
        "Service name \"" + serviceId.name() + "\" must match pattern \"" + ServiceTask.NAMING_PATTERN + "\"");
    }
    // resolve the path of the service in the logical directory
    return staticService
      ? manager.persistentServicesDirectory().resolve(serviceId.name())
      : manager.tempDirectory().resolve(String.format("%s_%s", serviceId.name(), serviceId.uniqueId()));
  }

  @Override
  public @NonNull ServiceInfoSnapshot serviceInfo() {
    return this.currentServiceInfo;
  }

  @Override
  public boolean valid() {
    return this.currentServiceInfo.lifeCycle() != ServiceLifeCycle.DELETED;
  }

  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    // check if the service is able to serve the request
    if (this.networkChannel != null) {
      var response = ChannelMessage.builder()
        .targetService(this.serviceId().name())
        .message("request_update_service_information")
        .channel(NetworkConstants.INTERNAL_MSG_CHANNEL)
        .build()
        .sendSingleQuery();
      return response == null ? this.currentServiceInfo : response.content().readObject(ServiceInfoSnapshot.class);
    } else {
      return this.currentServiceInfo;
    }
  }

  @Override
  public void addServiceTemplate(@NonNull ServiceTemplate serviceTemplate) {
    this.waitingTemplates.add(serviceTemplate);
  }

  @Override
  public void addServiceRemoteInclusion(@NonNull ServiceRemoteInclusion serviceRemoteInclusion) {
    this.waitingRemoteInclusions.add(serviceRemoteInclusion);
  }

  @Override
  public void addServiceDeployment(@NonNull ServiceDeployment serviceDeployment) {
    this.waitingDeployments.add(serviceDeployment);
  }

  @Override
  public @NonNull ServiceConsoleLogCache serviceConsoleLogCache() {
    return this.logCache;
  }

  @Override
  public void deleteFiles() {
    // stop the process & delete the configured files
    this.doDelete();
    // delete the folder of the service, even if it's a static service
    FileUtil.delete(this.serviceDirectory);
    // push the new lifecycle
    this.pushServiceInfoSnapshotUpdate(ServiceLifeCycle.DELETED);
  }

  @Override
  public void updateLifecycle(@NonNull ServiceLifeCycle lifeCycle) {
    try {
      // prevent multiple service updates at the same time
      this.lifecycleLock.lock();
      // prevent changing the lifecycle to an incompatible lifecycle
      if (!this.lifeCycle().canChangeTo(lifeCycle)) {
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
            if (this.lifeCycle() == ServiceLifeCycle.PREPARED && this.canStartNow()) {
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
            if (this.serviceConfiguration().autoDeleteOnStop()) {
              this.doDelete();
              // update the current service info
              this.pushServiceInfoSnapshotUpdate(ServiceLifeCycle.DELETED);
            } else if (this.lifeCycle() == ServiceLifeCycle.RUNNING) {
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
    this.includeWaitingServiceTemplates(true);
  }

  @Override
  public void includeWaitingServiceInclusions() {
    ServiceRemoteInclusion inclusion;
    while ((inclusion = this.waitingRemoteInclusions.poll()) != null) {
      // prepare the connection from which we load the inclusion
      var getRequest = Unirest.get(inclusion.url());
      // put the given http headers
      if (inclusion.properties().contains("httpHeaders")) {
        var headers = inclusion.properties().getDocument("httpHeaders");
        for (var key : headers.keys()) {
          getRequest.header(key, headers.get(key).toString());
        }
      }
      // check if we should load the inclusion
      if (!this.eventManager.callEvent(new CloudServicePreLoadInclusionEvent(this, inclusion, getRequest))
        .cancelled()) {
        // get a target path based on the download url
        var destination = INCLUSION_TEMP_DIR.resolve(
          Base64.getEncoder().encodeToString(inclusion.url().getBytes(StandardCharsets.UTF_8)).replace('/', '_'));
        // download the file from the given url to the temp path if it does not exist
        if (Files.notExists(destination)) {
          try {
            // we only support success codes for downloading the file
            getRequest.asFile(destination.toString());
          } catch (UnirestException exception) {
            LOGGER.severe("Unable to download inclusion from %s to %s", exception.getCause(), inclusion.url(),
              destination);
            continue;
          }
        }
        // resolve the desired output path
        var target = this.serviceDirectory.resolve(inclusion.destination());
        FileUtil.ensureChild(this.serviceDirectory, target);
        // copy the file to the desired output path
        FileUtil.copy(destination, target);
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
      for (var deployment : this.waitingDeployments) {
        this.executeDeployment(deployment);
      }
    }
  }

  protected void doDelete() {
    // stop the process if it's running
    if (this.currentServiceInfo.lifeCycle() == ServiceLifeCycle.RUNNING || this.alive()) {
      this.stopProcess();
    }
    // execute all deployments which are still waiting - delete all requested files before that
    this.doRemoveFilesAfterStop();
    this.removeAndExecuteDeployments();
    // remove the current directory if the service is not static
    if (!this.serviceConfiguration().staticService()) {
      FileUtil.delete(this.serviceDirectory);
    }
  }

  @Override
  public @NonNull Queue<ServiceRemoteInclusion> waitingIncludes() {
    return this.waitingRemoteInclusions;
  }

  @Override
  public @NonNull Queue<ServiceTemplate> waitingTemplates() {
    return this.waitingTemplates;
  }

  @Override
  public @NonNull Queue<ServiceDeployment> waitingDeployments() {
    return this.waitingDeployments;
  }

  @Override
  public @NonNull ServiceLifeCycle lifeCycle() {
    return this.currentServiceInfo.lifeCycle();
  }

  @Override
  public @NonNull CloudServiceManager cloudServiceManager() {
    return this.cloudServiceManager;
  }

  @Override
  public @NonNull ServiceConfiguration serviceConfiguration() {
    return this.currentServiceInfo.configuration();
  }

  @Override
  public @NonNull ServiceId serviceId() {
    return this.currentServiceInfo.serviceId();
  }

  @Override
  public @NonNull String connectionKey() {
    return this.connectionKey;
  }

  @Override
  public @NonNull Path directory() {
    return this.serviceDirectory;
  }

  @Override
  public @Nullable NetworkChannel networkChannel() {
    return this.networkChannel;
  }

  @Override
  public void networkChannel(@Nullable NetworkChannel channel) {
    Preconditions.checkArgument(this.networkChannel == null || channel == null);
    // close the channel if the new channel is null
    if (this.networkChannel != null) {
      this.networkChannel.close();
      this.currentServiceInfo.connectedTime(0);
    } else {
      this.currentServiceInfo.connectedTime(System.currentTimeMillis());
    }
    // set the new channel
    this.networkChannel = channel;
  }

  @Override
  public @NonNull ServiceInfoSnapshot lastServiceInfoSnapshot() {
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
  public void updateServiceInfoSnapshot(@NonNull ServiceInfoSnapshot serviceInfoSnapshot) {
    this.lastServiceInfo = this.currentServiceInfo;
    this.currentServiceInfo = serviceInfoSnapshot;
  }

  @Override
  public Queue<String> cachedLogMessages() {
    return this.serviceConsoleLogCache().cachedLogMessages();
  }

  @Override
  public boolean toggleScreenEvents(@NonNull ChannelMessageSender channelMessageSender, @NonNull String channel) {
    var target = channelMessageSender.toTarget();
    if (this.logTargets.remove(target) != null) {
      return false;
    }
    // this returns always true, just to inline it.
    return this.logTargets.put(target, channel) == null;
  }

  protected @NonNull Configuration nodeConfiguration() {
    return this.nodeInstance.config();
  }

  protected void includeWaitingServiceTemplates(boolean force) {
    this.waitingTemplates.stream()
      .filter(template -> {
        // always allow manual requests & non-static service copies
        if (force || !this.serviceConfiguration().staticService()) {
          return true;
        }
        // only allow this template to be copied if explicitly defined
        return template.alwaysCopyToStaticServices();
      })
      .sorted()
      .forEachOrdered(template -> {
        // remove the entry
        this.waitingTemplates.remove(template);
        // check if we should load the template
        var storage = template.storage().wrappedStorage();
        if (!this.eventManager.callEvent(new CloudServiceTemplateLoadEvent(this, storage, template)).cancelled()) {
          // the event is not cancelled - copy the template
          storage.copy(template, this.serviceDirectory);
        }
      });
  }

  protected void executeDeployment(@NonNull ServiceDeployment deployment) {
    // check if we should execute the deployment
    var storage = deployment.template().storage().wrappedStorage();
    if (!this.eventManager.callEvent(new CloudServiceDeploymentEvent(this, storage, deployment)).cancelled()) {
      // execute the deployment
      storage.deployDirectory(this.serviceDirectory, deployment.template(), path -> {
        // normalize the name of the path
        var fileName = Files.isDirectory(path)
          ? path.getFileName().toString() + '/'
          : path.getFileName().toString();
        // check if the file is ignored
        return deployment.excludes().stream().noneMatch(pattern -> fileName.matches(pattern.replace("*", "(.*)")))
          && !DEFAULT_DEPLOYMENT_EXCLUSIONS.contains(fileName);
      });
    }
  }

  protected void doRemoveFilesAfterStop() {
    for (var file : this.serviceConfiguration.deletedFilesAfterStop()) {
      FileUtil.delete(this.serviceDirectory.resolve(file));
    }
  }

  protected boolean preLifecycleChange(@NonNull ServiceLifeCycle targetLifecycle) {
    return !this.eventManager.callEvent(new CloudServicePreLifecycleEvent(this, targetLifecycle)).cancelled();
  }

  protected void pushServiceInfoSnapshotUpdate(@NonNull ServiceLifeCycle lifeCycle) {
    // save the current service info
    this.lastServiceInfo = this.currentServiceInfo;
    // update the current info
    this.currentServiceInfo = new ServiceInfoSnapshot(
      this.lastServiceInfo.creationTime(),
      this.lastServiceInfo.address(),
      this.lastServiceInfo.connectAddress(),
      this.alive() ? this.lastServiceInfo.processSnapshot() : ProcessSnapshot.empty(),
      this.lastServiceInfo.configuration(),
      this.networkChannel == null ? -1 : this.lastServiceInfo.connectedTime(),
      lifeCycle,
      this.lastServiceInfo.properties());
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
      .buffer(DataBuf.empty().writeObject(this.lastServiceInfo.lifeCycle()).writeObject(this.currentServiceInfo))
      .build()
      .send();
  }

  protected boolean canStartNow() {
    // check jvm heap size
    if (this.cloudServiceManager.currentUsedHeapMemory()
      + this.serviceConfiguration().processConfig().maxHeapMemorySize()
      >= this.nodeConfiguration().maxMemory()) {
      // schedule a retry
      if (this.nodeConfiguration().runBlockedServiceStartTryLaterAutomatic()) {
        CloudNet.instance().mainThread().runTask(this::start);
      } else {
        LOGGER.info(I18n.trans("cloud-service-manager-max-memory-error"));
      }
      // no starting now
      return false;
    }
    // check for cpu usage
    if (CPUUsageResolver.systemCPUUsage() >= this.nodeConfiguration().maxCPUUsageToStartServices()) {
      // schedule a retry
      if (this.nodeConfiguration().runBlockedServiceStartTryLaterAutomatic()) {
        CloudNet.instance().mainThread().runTask(this::start);
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
    var firstStartup = Files.notExists(this.serviceDirectory);
    FileUtil.createDirectory(this.serviceDirectory);
    // write the configuration file for the service
    var listeners = this.nodeConfiguration().identity().listeners();
    JsonDocument.newDocument()
      .append("connectionKey", this.connectionKey())
      .append("serviceInfoSnapshot", this.currentServiceInfo)
      .append("serviceConfiguration", this.serviceConfiguration())
      .append("sslConfiguration", this.nodeConfiguration().serverSSLConfig())
      .append("targetListener", listeners.get(ThreadLocalRandom.current().nextInt(listeners.size())))
      .write(this.serviceDirectory.resolve(WRAPPER_CONFIG_PATH));
    // load the ssl configuration if enabled
    var sslConfiguration = this.nodeConfiguration().serverSSLConfig();
    if (sslConfiguration.enabled()) {
      this.copySslConfiguration(sslConfiguration);
    }
    // add all components
    this.waitingTemplates.addAll(this.serviceConfiguration.templates());
    this.waitingDeployments.addAll(this.serviceConfiguration.deployments());
    this.waitingRemoteInclusions.addAll(this.serviceConfiguration.includes());
    // load the inclusions
    this.includeWaitingServiceInclusions();
    // check if we should load the templates of the service
    this.includeWaitingServiceTemplates(firstStartup);
    // update the service configuration
    this.serviceConfigurationPreparer.configure(this.nodeInstance, this);
  }

  protected void copySslConfiguration(@NonNull SSLConfiguration configuration) {
    var wrapperDir = this.serviceDirectory.resolve(".wrapper");
    // copy the certificate if available
    if (configuration.certificatePath() != null && Files.exists(configuration.certificatePath())) {
      FileUtil.copy(configuration.certificatePath(), wrapperDir.resolve("certificate"));
    }
    // copy the private key if available
    if (configuration.privateKeyPath() != null && Files.exists(configuration.privateKeyPath())) {
      FileUtil.copy(configuration.privateKeyPath(), wrapperDir.resolve("privateKey"));
    }
    // copy the trust certificate if available
    if (configuration.trustCertificatePath() != null && Files.exists(configuration.trustCertificatePath())) {
      FileUtil.copy(configuration.trustCertificatePath(), wrapperDir.resolve("trustCertificate"));
    }
  }

  protected abstract void startProcess();

  protected abstract void stopProcess();
}
