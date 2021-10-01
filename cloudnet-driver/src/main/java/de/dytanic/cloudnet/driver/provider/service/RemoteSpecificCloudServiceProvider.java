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

import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoteSpecificCloudServiceProvider implements SpecificCloudServiceProvider {

  // identity information
  private final String name;
  private final UUID uniqueId;
  // rpc
  private final RPCSender providerSender;
  private final RPCSender thisProviderSender;
  // provider
  private final GeneralCloudServiceProvider provider;

  public RemoteSpecificCloudServiceProvider(GeneralCloudServiceProvider provider, RPCSender providerSender, UUID id) {
    this.provider = provider;
    // rpc
    this.providerSender = providerSender;
    this.thisProviderSender = providerSender
      .getFactory()
      .providerForClass(providerSender.getAssociatedComponent(), SpecificCloudServiceProvider.class);
    // identity
    this.name = null;
    this.uniqueId = id;
  }

  public RemoteSpecificCloudServiceProvider(GeneralCloudServiceProvider provider, RPCSender providerSender, String id) {
    this.provider = provider;
    // rpc
    this.providerSender = providerSender;
    this.thisProviderSender = providerSender
      .getFactory()
      .providerForClass(providerSender.getAssociatedComponent(), SpecificCloudServiceProvider.class);
    // identity
    this.name = id;
    this.uniqueId = null;
  }

  @Override
  public @Nullable ServiceInfoSnapshot getServiceInfoSnapshot() {
    return this.name == null
      ? this.provider.getCloudService(this.uniqueId)
      : this.provider.getCloudServiceByName(this.name);
  }

  @Override
  public boolean isValid() {
    //noinspection ConstantConditions
    return this.getBaseRPC().join(this.thisProviderSender.invokeMethod("isValid")).fireSync();
  }

  @Override
  public @Nullable ServiceInfoSnapshot forceUpdateServiceInfo() {
    return this.getBaseRPC().join(this.thisProviderSender.invokeMethod("forceUpdateServiceInfo")).fireSync();
  }

  @Override
  public void addServiceTemplate(@NotNull ServiceTemplate template) {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("addServiceTemplate", template)).fireSync();
  }

  @Override
  public void addServiceRemoteInclusion(@NotNull ServiceRemoteInclusion inclusion) {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("addServiceRemoteInclusion", inclusion)).fireSync();
  }

  @Override
  public void addServiceDeployment(@NotNull ServiceDeployment deployment) {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("addServiceDeployment", deployment)).fireSync();
  }

  @Override
  public Queue<String> getCachedLogMessages() {
    return this.getBaseRPC().join(this.thisProviderSender.invokeMethod("getCachedLogMessages")).fireSync();
  }

  @Override
  public void setCloudServiceLifeCycle(@NotNull ServiceLifeCycle lifeCycle) {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("setCloudServiceLifeCycle", lifeCycle)).fireSync();
  }

  @Override
  public void restart() {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("restart")).fireSync();
  }

  @Override
  public void runCommand(@NotNull String command) {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("runCommand", command)).fireSync();
  }

  @Override
  public void includeWaitingServiceTemplates() {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("includeWaitingServiceTemplates")).fireSync();
  }

  @Override
  public void includeWaitingServiceInclusions() {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("includeWaitingServiceInclusions")).fireSync();
  }

  @Override
  public void deployResources(boolean removeDeployments) {
    this.getBaseRPC().join(this.thisProviderSender.invokeMethod("deployResources", removeDeployments)).fireSync();
  }

  protected @NotNull RPC getBaseRPC() {
    return this.name == null
      ? this.providerSender.invokeMethod("getSpecificProvider", this.uniqueId)
      : this.providerSender.invokeMethod("getSpecificProviderByName", this.name);
  }
}
