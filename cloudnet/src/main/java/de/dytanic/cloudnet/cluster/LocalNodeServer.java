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

package de.dytanic.cloudnet.cluster;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.DriverCommandSender;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.provider.service.EmptySpecificCloudServiceProvider;
import de.dytanic.cloudnet.provider.service.LocalNodeSpecificCloudServiceProvider;
import de.dytanic.cloudnet.service.ICloudService;
import java.util.ArrayList;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalNodeServer extends DefaultNodeServer implements NodeServer {

  private final CloudNet cloudNet;
  private final NodeServerProvider<? extends NodeServer> provider;

  protected LocalNodeServer(CloudNet cloudNet, NodeServerProvider<? extends NodeServer> provider) {
    this.cloudNet = cloudNet;
    this.provider = provider;

    this.setNodeInfoSnapshot(this.cloudNet.createClusterNodeInfoSnapshot());
    this.setNodeInfo(this.cloudNet.getConfig().getIdentity());
  }

  @Override
  public @NotNull NodeServerProvider<? extends NodeServer> getProvider() {
    return this.provider;
  }

  @Override
  public boolean isAvailable() {
    return this.cloudNet.isRunning();
  }

  @Override
  public @NotNull String[] sendCommandLine(@NotNull String commandLine) {
    Collection<String> result = new ArrayList<>();
    this.cloudNet.getCommandMap().dispatchCommand(new DriverCommandSender(result), commandLine);
    return result.toArray(new String[0]);
  }

  @Override
  public @NotNull CloudServiceFactory getCloudServiceFactory() {
    return this.cloudNet.getCloudServiceFactory();
  }

  @Override
  public @Nullable SpecificCloudServiceProvider getCloudServiceProvider(
    @NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    ICloudService service = this.cloudNet.getCloudServiceManager()
      .getCloudService(serviceInfoSnapshot.getServiceId().getUniqueId());
    return service == null
      ? EmptySpecificCloudServiceProvider.INSTANCE
      : new LocalNodeSpecificCloudServiceProvider(this.cloudNet, service);
  }

  @Override
  public void close() throws Exception {
    this.cloudNet.stop();
  }
}
