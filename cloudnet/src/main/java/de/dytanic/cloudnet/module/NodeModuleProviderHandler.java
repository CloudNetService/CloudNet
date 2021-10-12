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

package de.dytanic.cloudnet.module;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.module.DefaultModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.IModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public final class NodeModuleProviderHandler extends DefaultModuleProviderHandler implements IModuleProviderHandler {

  private final CloudNet nodeInstance;

  public NodeModuleProviderHandler(@NotNull CloudNet nodeInstance) {
    this.nodeInstance = nodeInstance;
  }

  @Override
  public void handlePostModuleStop(@NotNull IModuleWrapper moduleWrapper) {
    super.handlePostModuleStop(moduleWrapper);

    // unregister all listeners from the http server
    this.nodeInstance.getHttpServer().removeHandler(moduleWrapper.getClassLoader());
    // unregister all listeners added to the network handlers
    this.nodeInstance.getNetworkClient().getPacketRegistry().removeListeners(moduleWrapper.getClassLoader());
    this.nodeInstance.getNetworkServer().getPacketRegistry().removeListeners(moduleWrapper.getClassLoader());
    // unregister all listeners added to the network channels
    this.removeListeners(this.nodeInstance.getNetworkClient().getChannels(), moduleWrapper.getClassLoader());
    this.removeListeners(this.nodeInstance.getNetworkServer().getChannels(), moduleWrapper.getClassLoader());
    // unregister all listeners
    this.nodeInstance.getEventManager().unregisterListeners(moduleWrapper.getClassLoader());
    // unregister all commands
    // todo CloudNet.getInstance().getCommandMap().unregisterCommands(moduleWrapper.getClassLoader());
  }

  private void removeListeners(@NotNull Collection<INetworkChannel> channels, @NotNull ClassLoader loader) {
    for (INetworkChannel channel : channels) {
      channel.getPacketRegistry().removeListeners(loader);
    }
  }
}
