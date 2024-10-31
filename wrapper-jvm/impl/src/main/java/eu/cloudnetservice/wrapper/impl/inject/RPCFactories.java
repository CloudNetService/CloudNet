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
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.impl.network.rpc.generation.RPCInternalInstanceFactory;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.factory.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ClusterNodeProvider;
import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.wrapper.impl.database.WrapperDatabaseProvider;
import eu.cloudnetservice.wrapper.impl.provider.WrapperTemplateStorageProvider;
import jakarta.inject.Singleton;
import lombok.NonNull;

@SuppressWarnings("unused")
final class RPCFactories {

  private RPCFactories() {
    throw new UnsupportedOperationException();
  }

  @Factory
  @Singleton
  public static @NonNull ClusterNodeProvider provideClusterNodeProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return factory.newRPCBasedImplementationBuilder(ClusterNodeProvider.class)
      .implementConcreteMethods()
      .targetComponent(networkClient)
      .generateImplementation()
      .allocate();
  }

  @Factory
  @Singleton
  public static @NonNull ServiceTaskProvider provideServiceTaskProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return factory.newRPCBasedImplementationBuilder(ServiceTaskProvider.class)
      .implementConcreteMethods()
      .targetComponent(networkClient)
      .generateImplementation()
      .allocate();
  }

  @Factory
  @Singleton
  public static @NonNull GroupConfigurationProvider provideGroupConfigurationProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return factory.newRPCBasedImplementationBuilder(GroupConfigurationProvider.class)
      .implementConcreteMethods()
      .targetComponent(networkClient)
      .generateImplementation()
      .allocate();
  }

  @Factory
  @Singleton
  public static @NonNull CloudServiceFactory provideCloudServiceFactory(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return factory.newRPCBasedImplementationBuilder(CloudServiceFactory.class)
      .implementConcreteMethods()
      .targetComponent(networkClient)
      .generateImplementation()
      .allocate();
  }

  @Factory
  @Singleton
  public static @NonNull CloudServiceProvider provideCloudServiceProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return factory.newRPCBasedImplementationBuilder(CloudServiceProvider.class)
      .implementConcreteMethods()
      .targetComponent(networkClient)
      .generateImplementation()
      .allocate();
  }

  @Factory
  @Singleton
  public static @NonNull TemplateStorageProvider provideTemplateStorageProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient,
    @NonNull ComponentInfo componentInfo
  ) {
    return factory.newRPCBasedImplementationBuilder(WrapperTemplateStorageProvider.class)
      .superclass(TemplateStorageProvider.class)
      .targetComponent(networkClient)
      .generateImplementation()
      .withAdditionalConstructorParameters(
        RPCInternalInstanceFactory.SpecialArg.RPC_SENDER,
        componentInfo,
        networkClient,
        RPCInternalInstanceFactory.SpecialArg.CHANNEL_SUPPLIER)
      .allocate();
  }

  @Factory
  @Singleton
  public static @NonNull DatabaseProvider provideDatabaseProvider(
    @NonNull RPCFactory factory,
    @NonNull NetworkClient networkClient
  ) {
    return factory.newRPCBasedImplementationBuilder(WrapperDatabaseProvider.class)
      .superclass(DatabaseProvider.class)
      .targetComponent(networkClient)
      .generateImplementation()
      .withAdditionalConstructorParameters(
        RPCInternalInstanceFactory.SpecialArg.RPC_SENDER,
        RPCInternalInstanceFactory.SpecialArg.CHANNEL_SUPPLIER)
      .allocate();
  }
}
