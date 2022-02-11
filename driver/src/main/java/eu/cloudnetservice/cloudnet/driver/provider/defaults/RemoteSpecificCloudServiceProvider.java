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

package eu.cloudnetservice.cloudnet.driver.provider.defaults;

import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.cloudnet.driver.network.NetworkChannel;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPC;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.cloudnet.driver.service.ServiceDeployment;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider for a cloud services which is not running on the local service. This provider uses chained rpc calls to
 * execute the requested method calls.
 *
 * @since 4.0
 */
public class RemoteSpecificCloudServiceProvider implements SpecificCloudServiceProvider {

  // identity information
  private final String name;
  private final UUID uniqueId;
  // rpc
  private final RPCSender providerSender;
  private final RPCSender thisProviderSender;
  private final Supplier<NetworkChannel> channelSupplier;
  // provider
  private final CloudServiceProvider provider;

  /**
   * Constructs a new remote cloud service provider based on the given unique id.
   *
   * @param provider        the general provider to which the target service of this provider is known.
   * @param providerSender  the rpc sender to execute method calls in the general provider.
   * @param channelSupplier the supplier for the channel of the component on which the service is running.
   * @param id              the unique id of the service targeted by this provider.
   * @throws NullPointerException if one of the given parameter is null.
   */
  public RemoteSpecificCloudServiceProvider(
    @NonNull CloudServiceProvider provider,
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

  /**
   * Constructs a new remote cloud service provider based on the given name. Note that the provider is not directly
   * unique bound to a service! If a service with the same name starts (after the current target was stopped) this
   * provider will target the new service.
   *
   * @param provider        the general provider to which the target service of this provider is known.
   * @param providerSender  the rpc sender to execute method calls in the general provider.
   * @param channelSupplier the supplier for the channel of the component on which the service is running.
   * @param id              the name of the service targeted by this provider.
   * @throws NullPointerException if one of the given parameter is null.
   */
  public RemoteSpecificCloudServiceProvider(
    @NonNull CloudServiceProvider provider,
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

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ServiceInfoSnapshot serviceInfo() {
    return this.name == null
      ? this.provider.service(this.uniqueId)
      : this.provider.serviceByName(this.name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean valid() {
    return this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("valid"))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    return this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("forceUpdateServiceInfo"))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addServiceTemplate(@NonNull ServiceTemplate template) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("addServiceTemplate", template))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addServiceRemoteInclusion(@NonNull ServiceRemoteInclusion inclusion) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("addServiceRemoteInclusion", inclusion))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addServiceDeployment(@NonNull ServiceDeployment deployment) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("addServiceDeployment", deployment))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Queue<String> cachedLogMessages() {
    return this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("cachedLogMessages"))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean toggleScreenEvents(@NonNull ChannelMessageSender channelMessageSender, @NonNull String channel) {
    return this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("toggleScreenEvents", channelMessageSender, channel))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteFiles() {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("deleteFiles"))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateLifecycle(@NonNull ServiceLifeCycle lifeCycle) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("updateLifecycle", lifeCycle))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void restart() {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("restart"))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void runCommand(@NonNull String command) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("runCommand", command))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void includeWaitingServiceTemplates() {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("includeWaitingServiceTemplates"))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void includeWaitingServiceInclusions() {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("includeWaitingServiceInclusions"))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deployResources(boolean removeDeployments) {
    this.baseRPC()
      .join(this.thisProviderSender.invokeMethod("deployResources", removeDeployments))
      .fireSync(this.channelSupplier.get());
  }

  /**
   * Constructs the base rpc to obtain the provider for a service on the remote component. This method is used to
   * construct the base rpc for chained method calls against the service providers on the remote component.
   * <p>
   * This method either uses the given name of the service or the unique id (the unique id will be preferred over the
   * name, meaning that if both identifiers are available the unique id will be used).
   *
   * @return the base rpc to obtain the service provider on the remote node.
   */
  protected @NonNull RPC baseRPC() {
    return this.uniqueId != null
      ? this.providerSender.invokeMethod("specificProvider", this.uniqueId)
      : this.providerSender.invokeMethod("specificProviderByName", this.name);
  }
}
