/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl.inject;

import dev.derklaro.aerogel.auto.Factory;
import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.impl.network.netty.client.NettyNetworkClient;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.impl.network.NetworkClientChannelHandler;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import lombok.NonNull;

@SuppressWarnings("unused")
final class BootFactories {

  private BootFactories() {
    throw new UnsupportedOperationException();
  }

  @Factory
  @Singleton
  public static @NonNull ComponentInfo provideComponentInfo(@NonNull WrapperConfiguration configuration) {
    var serviceId = configuration.serviceConfiguration().serviceId();
    return new ComponentInfo(DriverEnvironment.WRAPPER, serviceId.name(), serviceId.nodeUniqueId());
  }

  @Factory
  @Singleton
  public static @NonNull NetworkClient provideNetworkClient(
    @NonNull ComponentInfo componentInfo,
    @NonNull WrapperConfiguration configuration,
    @NonNull Provider<NetworkClientChannelHandler> handlerProvider
  ) {
    return new NettyNetworkClient(componentInfo, handlerProvider::get, configuration.sslConfiguration());
  }
}
