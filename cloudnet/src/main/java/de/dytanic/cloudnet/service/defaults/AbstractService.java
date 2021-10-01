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
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import de.dytanic.cloudnet.event.service.CloudServiceDeploymentEvent;
import de.dytanic.cloudnet.event.service.CloudServicePreLoadInclusionEvent;
import de.dytanic.cloudnet.event.service.CloudServiceTemplateLoadEvent;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public abstract class AbstractService implements ICloudService {

  protected static final Logger LOGGER = LogManager.getLogger(AbstractService.class);

  protected static final Collection<String> DEFAULT_DEPLOYMENT_EXCLUSIONS = Arrays.asList("wrapper.jar", ".wrapper/");
  protected static final Path INCLUSION_TEMP_DIR = Paths.get(
    System.getProperty("cloudnet.tempDir", "temp"),
    "inclusions");

  protected final IEventManager eventManager;

  protected final String connectionKey;
  protected final Path serviceDirectory;
  protected final ICloudServiceManager cloudServiceManager;

  protected final Queue<ServiceTemplate> waitingTemplates = new ConcurrentLinkedQueue<>();
  protected final Queue<ServiceDeployment> waitingDeployments = new ConcurrentLinkedQueue<>();
  protected final Queue<ServiceRemoteInclusion> waitingRemoteInclusions = new ConcurrentLinkedQueue<>();

  protected volatile INetworkChannel networkChannel;

  protected volatile ServiceInfoSnapshot lastServiceInfo;
  protected volatile ServiceInfoSnapshot currentServiceInfo;

  protected AbstractService(
    @NotNull ServiceConfiguration configuration,
    @NotNull ICloudServiceManager manager,
    @NotNull IEventManager eventManager
  ) {
    this.eventManager = eventManager;
    this.cloudServiceManager = manager;

    this.connectionKey = StringUtil.generateRandomString(64);
    this.serviceDirectory = resolveServicePath(configuration.getServiceId(), manager, configuration.isStaticService());

    this.currentServiceInfo = this.lastServiceInfo = new ServiceInfoSnapshot(
      System.currentTimeMillis(),
      -1,
      new HostAndPort(CloudNet.getInstance().getConfig().getHostAddress(), configuration.getPort()),
      new HostAndPort(CloudNet.getInstance().getConfig().getConnectHostAddress(), configuration.getPort()),
      ServiceLifeCycle.DEFINED,
      ProcessSnapshot.empty(),
      configuration,
      configuration.getProperties());
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
  public @Nullable ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.currentServiceInfo;
  }

  @Override
  public boolean isValid() {
    return this.currentServiceInfo.getLifeCycle() != ServiceLifeCycle.DELETED;
  }

  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    return null;
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
  public void restart() {
    this.stop();
    this.start();
  }

  @Override
  public void includeWaitingServiceTemplates() {
    this.waitingTemplates.stream().sorted().forEachOrdered(template -> {
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

  @Override
  public void includeWaitingServiceInclusions() {
    ServiceRemoteInclusion inclusion;
    while ((inclusion = this.waitingRemoteInclusions.poll()) != null) {
      // prepare the connection from which we load the inclusion
      HttpURLConnection con = HttpConnectionProvider.provideConnection(inclusion.getUrl());
      con.setUseCaches(false);
      // put the given http headers
      if (inclusion.getProperties() != null && inclusion.getProperties().contains("httpHeaders")) {
        JsonDocument headers = inclusion.getProperties().getDocument("httpHeaders");
        for (String key : headers.keys()) {
          con.setRequestProperty(key, headers.getElement(key).toString());
        }
      }
      // check if we should load the inclusion
      if (!this.eventManager.callEvent(new CloudServicePreLoadInclusionEvent(this, inclusion, con)).isCancelled()) {
        // get a target path based on the download url
        Path destination = INCLUSION_TEMP_DIR.resolve(
          Base64.getEncoder().encodeToString(inclusion.getUrl().getBytes(StandardCharsets.UTF_8)).replace('/', '_'));
        // download the file from the given url to the temp path if it does not exists
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
        try {
          FileUtils.copy(destination, target);
        } catch (IOException exception) {
          LOGGER.severe("Unable to copy the inclusion %s from %s to %s",
            exception,
            inclusion.getUrl(),
            destination,
            target);
        }
      }
    }
  }

  @Override
  public void deployResources(boolean removeDeployments) {
    ServiceDeployment deployment;
    while ((deployment = this.waitingDeployments.poll()) != null) {
      // check if we should execute the deployment
      TemplateStorage storage = deployment.getTemplate().storage().getWrappedStorage();
      if (!this.eventManager.callEvent(new CloudServiceDeploymentEvent(this, storage, deployment)).isCancelled()) {
        // store the excludes locally to allow access in the lambda
        Collection<String> excludes = deployment.getExcludes();
        // execute the deployment
        storage.deploy(this.serviceDirectory, deployment.getTemplate(), path -> {
          // normalize the name of the path
          String fileName = Files.isDirectory(path)
            ? path.getFileName().toString() + '/'
            : path.getFileName().toString();
          // check if the file is ignored
          return !excludes.contains(fileName) && !DEFAULT_DEPLOYMENT_EXCLUSIONS.contains(fileName);
        });
      }
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
    if (channel == null && this.networkChannel != null) {
      this.networkChannel.close();
    }
    // set the new channel
    this.networkChannel = channel;
  }

  @Override
  public @NotNull ServiceInfoSnapshot getLastServiceInfoSnapshot() {
    return this.lastServiceInfo;
  }

  @Override
  public void updateServiceInfoSnapshot(@NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    this.currentServiceInfo = Preconditions.checkNotNull(serviceInfoSnapshot, "serviceInfoSnapshot");
  }

  @Override
  public Queue<String> getCachedLogMessages() {
    return this.getServiceConsoleLogCache().getCachedLogMessages();
  }

  protected @NotNull ServiceInfoSnapshot generateServiceInfoSnapshot(@NotNull ServiceLifeCycle lifeCycle) {
    return new ServiceInfoSnapshot(
      this.currentServiceInfo.getCreationTime(),
      this.networkChannel == null ? -1 : this.currentServiceInfo.getConnectedTime(),
      this.currentServiceInfo.getAddress(),
      this.currentServiceInfo.getConnectAddress(),
      Preconditions.checkNotNull(lifeCycle, "lifecycle"),
      this.isAlive() ? this.currentServiceInfo.getProcessSnapshot() : ProcessSnapshot.empty(),
      this.currentServiceInfo.getConfiguration(),
      this.currentServiceInfo.getProperties());
  }
}
