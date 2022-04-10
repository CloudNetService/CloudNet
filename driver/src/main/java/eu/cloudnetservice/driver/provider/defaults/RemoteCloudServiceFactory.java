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

package eu.cloudnetservice.driver.provider.defaults;

import eu.cloudnetservice.driver.network.NetworkChannel;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.RPCSender;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A cloud service factory implementation which sends the creation requests to the channel supplied by the given
 * supplier via rpc.
 *
 * @since 4.0
 */
public class RemoteCloudServiceFactory implements CloudServiceFactory {

  private final RPCSender rpcSender;
  private final Supplier<NetworkChannel> channelSupplier;

  /**
   * Constructs a new remote cloud service factory instance.
   *
   * @param channelSupplier the supplier of the channel to send the creation requests to.
   * @param factory         the rpc factory used by the current environment to obtain a rpc sender.
   * @throws NullPointerException if either the given channel supplier or rpc factory is null.
   */
  public RemoteCloudServiceFactory(@NonNull Supplier<NetworkChannel> channelSupplier, @NonNull RPCFactory factory) {
    this.channelSupplier = channelSupplier;
    this.rpcSender = factory.providerForClass(null, CloudServiceFactory.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable ServiceInfoSnapshot createCloudService(@NonNull ServiceConfiguration config) {
    return this.rpcSender.invokeMethod("createCloudService", config).fireSync(this.channelSupplier.get());
  }
}
