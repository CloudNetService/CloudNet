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

package de.dytanic.cloudnet.driver.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.api.ServiceDriverAPIResponse;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoteSpecificCloudServiceProvider implements SpecificCloudServiceProvider, DriverAPIUser {

  private final INetworkChannel channel;
  private final GeneralCloudServiceProvider provider;
  private UUID uniqueId;
  private String name;
  private ServiceInfoSnapshot serviceInfoSnapshot;

  public RemoteSpecificCloudServiceProvider(INetworkChannel channel, GeneralCloudServiceProvider provider,
    UUID uniqueId) {
    this.channel = channel;
    this.provider = provider;
    this.uniqueId = uniqueId;
  }

  public RemoteSpecificCloudServiceProvider(INetworkChannel channel, GeneralCloudServiceProvider provider,
    String name) {
    this.channel = channel;
    this.provider = provider;
    this.name = name;
  }

  public RemoteSpecificCloudServiceProvider(INetworkChannel channel, ServiceInfoSnapshot serviceInfoSnapshot) {
    this.channel = channel;
    this.provider = null;
    this.serviceInfoSnapshot = serviceInfoSnapshot;
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot getServiceInfoSnapshot() {
    if (this.serviceInfoSnapshot != null) {
      return this.serviceInfoSnapshot;
    }
    return this.getServiceInfoSnapshotAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public boolean isValid() {
    return this.isValidAsync().get(5, TimeUnit.SECONDS, false);
  }

  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    return this.forceUpdateServiceInfoAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<ServiceInfoSnapshot> getServiceInfoSnapshotAsync() {
    if (this.serviceInfoSnapshot != null || this.provider == null) {
      return CompletedTask.create(this.serviceInfoSnapshot);
    }
    if (this.uniqueId != null) {
      return this.provider.getCloudServiceAsync(this.uniqueId);
    }
    if (this.name != null) {
      return this.provider.getCloudServiceByNameAsync(this.name);
    }
    throw new IllegalArgumentException("Cannot get ServiceInfoSnapshot without uniqueId or name");
  }

  @Override
  public @NotNull ITask<Boolean> isValidAsync() {
    if (this.serviceInfoSnapshot != null) {
      return CompletedTask.create(true);
    }
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.IS_CLOUD_SERVICE_VALID,
      this::writeDefaults,
      packet -> packet.getBuffer().readEnumConstant(ServiceDriverAPIResponse.class)
        != ServiceDriverAPIResponse.SERVICE_NOT_FOUND
    );
  }

  @Override
  public @NotNull ITask<ServiceInfoSnapshot> forceUpdateServiceInfoAsync() {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.FORCE_UPDATE_SERVICE,
      this::writeDefaults,
      packet -> this.readDefaults(packet.getBuffer()).readOptionalObject(ServiceInfoSnapshot.class)
    );
  }

  @Override
  public void addServiceTemplate(@NotNull ServiceTemplate serviceTemplate) {
    this.addServiceTemplateAsync(serviceTemplate).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Void> addServiceTemplateAsync(@NotNull ServiceTemplate serviceTemplate) {
    Preconditions.checkNotNull(serviceTemplate);

    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.ADD_SERVICE_TEMPLATE_TO_CLOUD_SERVICE,
      buffer -> this.writeDefaults(buffer).writeObject(serviceTemplate),
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  public void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
    this.addServiceRemoteInclusionAsync(serviceRemoteInclusion).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Void> addServiceRemoteInclusionAsync(@NotNull ServiceRemoteInclusion serviceRemoteInclusion) {
    Preconditions.checkNotNull(serviceRemoteInclusion);

    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.ADD_SERVICE_REMOTE_INCLUSION_TO_CLOUD_SERVICE,
      buffer -> this.writeDefaults(buffer).writeObject(serviceRemoteInclusion),
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  public void addServiceDeployment(@NotNull ServiceDeployment serviceDeployment) {
    this.addServiceDeploymentAsync(serviceDeployment).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Void> addServiceDeploymentAsync(@NotNull ServiceDeployment serviceDeployment) {
    Preconditions.checkNotNull(serviceDeployment);

    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.ADD_SERVICE_DEPLOYMENT_TO_CLOUD_SERVICE,
      buffer -> this.writeDefaults(buffer).writeObject(serviceDeployment),
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  public Queue<String> getCachedLogMessages() {
    return this.getCachedLogMessagesAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Queue<String>> getCachedLogMessagesAsync() {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_CACHED_LOG_MESSAGES_FROM_CLOUD_SERVICE,
      this::writeDefaults,
      packet -> new LinkedBlockingQueue<>(this.readDefaults(packet.getBuffer()).readStringCollection())
    );
  }

  @Override
  public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
    this.setCloudServiceLifeCycleAsync(lifeCycle).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Void> setCloudServiceLifeCycleAsync(@NotNull ServiceLifeCycle lifeCycle) {
    Preconditions.checkNotNull(lifeCycle);

    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.SET_CLOUD_SERVICE_LIFE_CYCLE,
      buffer -> this.writeDefaults(buffer).writeEnumConstant(lifeCycle),
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  public void restart() {
    this.restartAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Void> restartAsync() {
    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.RESTART_CLOUD_SERVICE,
      this::writeDefaults,
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  public void kill() {
    this.killAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Void> killAsync() {
    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.KILL_CLOUD_SERVICE,
      this::writeDefaults,
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  public void runCommand(@NotNull String command) {
    this.runCommandAsync(command).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Void> runCommandAsync(@NotNull String command) {
    Preconditions.checkNotNull(command);

    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.RUN_COMMAND_ON_CLOUD_SERVICE,
      buffer -> this.writeDefaults(buffer).writeString(command),
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  public void includeWaitingServiceTemplates() {
    this.includeWaitingServiceTemplatesAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public void includeWaitingServiceInclusions() {
    this.includeWaitingServiceInclusionsAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public void deployResources(boolean removeDeployments) {
    this.deployResourcesAsync(removeDeployments).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Void> includeWaitingServiceTemplatesAsync() {
    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.INCLUDE_WAITING_TEMPLATES_ON_CLOUD_SERVICE,
      this::writeDefaults,
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  @NotNull
  public ITask<Void> includeWaitingServiceInclusionsAsync() {
    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.INCLUDE_WAITING_INCLUSIONS_ON_CLOUD_SERVICE,
      this::writeDefaults,
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  @Override
  @NotNull
  public ITask<Void> deployResourcesAsync(boolean removeDeployments) {
    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.DEPLOY_RESOURCES_ON_CLOUD_SERVICE,
      buffer -> this.writeDefaults(buffer).writeBoolean(removeDeployments),
      packet -> this.readDefaults(packet.getBuffer())
    );
  }

  private ProtocolBuffer writeDefaults(ProtocolBuffer buffer) {
    return buffer.writeOptionalUUID(
      this.serviceInfoSnapshot != null ? this.serviceInfoSnapshot.getServiceId().getUniqueId() : this.uniqueId)
      .writeOptionalString(this.name);
  }

  private ProtocolBuffer readDefaults(ProtocolBuffer buffer) {
    ServiceDriverAPIResponse response = buffer.readEnumConstant(ServiceDriverAPIResponse.class);
    if (response == ServiceDriverAPIResponse.SERVICE_NOT_FOUND) {
      throw new IllegalArgumentException("The service of this provider doesn't exist");
    }
    return buffer;
  }

  @Override
  public INetworkChannel getNetworkChannel() {
    return this.channel;
  }
}
