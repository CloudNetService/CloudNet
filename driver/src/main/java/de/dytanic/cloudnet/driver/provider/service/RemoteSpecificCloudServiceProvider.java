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

package de.dytanic.cloudnet.driver.provider.service;

import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class RemoteSpecificCloudServiceProvider implements SpecificCloudServiceProvider {

  // identity information
  private final String name;
  private final UUID uniqueId;
  // rpc
  private final RPCSender providerSender;
  private final RPCSender thisProviderSender;
  private final Supplier<NetworkChannel> channelSupplier;
  // provider
  private final GeneralCloudServiceProvider provider;

  public RemoteSpecificCloudServiceProvider(
    @NonNull GeneralCloudServiceProvider provider,
    @NonNull RPCSender providerSender,
    @NonNull Supplier<NetworkChannel> channelSupplier,
    @NonNull UUID id
  ) {
    this.provider = provider;
    // rpc
    this.providerSender = providerSender;
    this.thisProviderSender = providerSender
      .factory()
      .providerForClass(null, SpecificCloudServiceProvider.class);
    this.channelSupplier = channelSupplier;
    // identity
    this.name = null;
    this.uniqueId = id;
  }

  public RemoteSpecificCloudServiceProvider(
    @NonNull GeneralCloudServiceProvider provider,
    @NonNull RPCSender providerSender,
    @NonNull Supplier<NetworkChannel> channelSupplier,
    @NonNull String id
  ) {
    this.provider = provider;
    // rpc
    this.providerSender = providerSender;
    this.thisProviderSender = providerSender
      .factory()
      .providerForClass(providerSender.associatedComponent(), SpecificCloudServiceProvider.class);
    this.channelSupplier = channelSupplier;
    // identity
    this.name = id;
    this.uniqueId = null;
  }

  @Override
  public @Nullable ServiceInfoSnapshot serviceInfo() {
    return this.name == null
      ? this.provider.service(this.uniqueId)
      : this.provider.serviceByName(this.name);
  }

  @Override
  public boolean valid() {
    return this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("valid"))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    return this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("forceUpdateServiceInfo"))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void addServiceTemplate(@NonNull ServiceTemplate template) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("addServiceTemplate", template))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void addServiceRemoteInclusion(@NonNull ServiceRemoteInclusion inclusion) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("addServiceRemoteInclusion", inclusion))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void addServiceDeployment(@NonNull ServiceDeployment deployment) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("addServiceDeployment", deployment))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public Queue<String> cachedLogMessages() {
    return this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("cachedLogMessages"))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public boolean toggleScreenEvents(@NonNull ChannelMessageSender channelMessageSender, @NonNull String channel) {
    return this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("toggleScreenEvents", channelMessageSender, channel))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void deleteFiles() {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("deleteFiles"))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void updateLifecycle(@NonNull ServiceLifeCycle lifeCycle) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("updateLifecycle", lifeCycle))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void restart() {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("restart"))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void runCommand(@NonNull String command) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("runCommand", command))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void includeWaitingServiceTemplates() {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("includeWaitingServiceTemplates"))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void includeWaitingServiceInclusions() {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("includeWaitingServiceInclusions"))
      .fireSync(this.channelSupplier.get());
  }

  @Override
  public void deployResources(boolean removeDeployments) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("deployResources", removeDeployments))
      .fireSync(this.channelSupplier.get());
  }

  protected @NonNull RPC baseRPC() {
    return this.name == null
      ? this.providerSender.invokeMethod("specificProvider", this.uniqueId)
      : this.providerSender.invokeMethod("specificProviderByName", this.name);
  }
}
