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

package eu.cloudnetservice.node.boot;

import dev.derklaro.aerogel.auto.Factory;
import eu.cloudnetservice.driver.CloudNetVersion;
import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.network.http.HttpServer;
import eu.cloudnetservice.driver.network.netty.client.NettyNetworkClient;
import eu.cloudnetservice.driver.network.netty.http.NettyHttpServer;
import eu.cloudnetservice.driver.network.netty.server.NettyNetworkServer;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.network.DefaultNetworkClientChannelHandler;
import eu.cloudnetservice.node.network.DefaultNetworkServerChannelHandler;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import lombok.NonNull;

@SuppressWarnings("unused") // methods are used for automatic binding detection
final class BootFactories {

  private BootFactories() {
    throw new UnsupportedOperationException();
  }

  @Factory
  @Singleton
  public static @NonNull HttpServer provideHttpServer(@NonNull Configuration configuration) {
    return new NettyHttpServer(configuration.webSSLConfig());
  }

  @Factory
  @Singleton
  public static @NonNull NetworkClient provideNetworkClient(
    @NonNull EventManager eventManager,
    @NonNull ComponentInfo componentInfo,
    @NonNull Configuration configuration,
    @NonNull Provider<DefaultNetworkClientChannelHandler> handlerProvider
  ) {
    return new NettyNetworkClient(eventManager, componentInfo, handlerProvider::get, configuration.clientSSLConfig());
  }

  @Factory
  @Singleton
  public static @NonNull NetworkServer provideNetworkServer(
    @NonNull EventManager eventManager,
    @NonNull ComponentInfo componentInfo,
    @NonNull Configuration configuration,
    @NonNull Provider<DefaultNetworkServerChannelHandler> handlerProvider
  ) {
    return new NettyNetworkServer(eventManager, componentInfo, handlerProvider::get, configuration.serverSSLConfig());
  }

  @Factory
  @Singleton
  public static @NonNull CloudNetVersion provideCloudNetVersion() {
    return CloudNetVersion.fromPackage(BootFactories.class.getPackage());
  }

  @Factory
  @Singleton
  public static @NonNull ComponentInfo providerComponentInfo(@NonNull Configuration configuration) {
    var nodeUniqueId = configuration.identity().uniqueId();
    return new ComponentInfo(DriverEnvironment.NODE, nodeUniqueId, nodeUniqueId);
  }

  @Factory
  @Singleton
  @Named("launcherDir")
  public static @NonNull Path provideLauncherDir() {
    var directory = System.getProperty("cloudnet.launcherdir", "launcher");
    return Path.of(directory);
  }
}
