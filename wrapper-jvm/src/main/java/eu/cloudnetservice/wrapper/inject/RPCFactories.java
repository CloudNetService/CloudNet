/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.inject;

import dev.derklaro.aerogel.auto.Factory;
import eu.cloudnetservice.driver.ComponentInfo;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.network.rpc.generation.GenerationContext;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import eu.cloudnetservice.wrapper.database.WrapperDatabaseProvider;
import eu.cloudnetservice.wrapper.permission.WrapperPermissionManagement;
import eu.cloudnetservice.wrapper.provider.WrapperCloudServiceProvider;
import eu.cloudnetservice.wrapper.provider.WrapperTemplateStorageProvider;
import jakarta.inject.Singleton;
import lombok.NonNull;

@SuppressWarnings("unused")
final class RPCFactories {

  private RPCFactories() {
    throw new UnsupportedOperationException();
  }

  private static @NonNull <T> T provideBasic(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient,
    @NonNull Class<T> baseClass
  ) {
    var context = GenerationContext.forClass(baseClass).component(networkClient).build();
    return factory.generateRPCBasedApi(baseClass, context).newInstance();
  }

  private static @NonNull <T> T provideSpecial(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient,
    @NonNull Class<T> baseClass,
    @NonNull Class<? extends T> implementingClass,
    @NonNull Object... specialConstructorArgs
  ) {
    var context = GenerationContext.forClass(implementingClass).component(networkClient).build();
    return factory.generateRPCBasedApi(baseClass, context).newInstance(specialConstructorArgs);
  }

  @Factory
  @Singleton
  public static @NonNull ClusterNodeProvider provideClusterNodeProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return provideBasic(factory, networkClient, ClusterNodeProvider.class);
  }

  @Factory
  @Singleton
  public static @NonNull ServiceTaskProvider provideServiceTaskProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return provideBasic(factory, networkClient, ServiceTaskProvider.class);
  }

  @Factory
  @Singleton
  public static @NonNull GroupConfigurationProvider provideGroupConfigurationProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return provideBasic(factory, networkClient, GroupConfigurationProvider.class);
  }

  @Factory
  @Singleton
  public static @NonNull CloudServiceFactory provideCloudServiceFactory(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return provideBasic(factory, networkClient, CloudServiceFactory.class);
  }

  @Factory
  @Singleton
  public static @NonNull TemplateStorageProvider provideTemplateStorageProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient,
    @NonNull ComponentInfo componentInfo
  ) {
    return provideSpecial(
      factory,
      networkClient,
      TemplateStorageProvider.class,
      WrapperTemplateStorageProvider.class,
      componentInfo, networkClient);
  }

  @Factory
  @Singleton
  public static @NonNull DatabaseProvider provideDatabaseProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return provideSpecial(factory, networkClient, DatabaseProvider.class, WrapperDatabaseProvider.class);
  }

  @Factory
  @Singleton
  public static @NonNull CloudServiceProvider provideCloudServiceProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return provideSpecial(factory, networkClient, CloudServiceProvider.class, WrapperCloudServiceProvider.class);
  }

  @Factory
  @Singleton
  public static @NonNull PermissionManagement providePermissionManagement(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient,
    @NonNull EventManager eventManager,
    @NonNull WrapperConfiguration configuration
  ) {
    return provideSpecial(
      factory,
      networkClient,
      PermissionManagement.class,
      WrapperPermissionManagement.class,
      eventManager, configuration.serviceConfiguration());
  }
}
