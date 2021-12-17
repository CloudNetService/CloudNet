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

package eu.cloudnetservice.cloudnet.modules.labymod.node;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreProcessStartEvent;
import eu.cloudnetservice.cloudnet.modules.labymod.LabyModManagement;
import eu.cloudnetservice.cloudnet.modules.labymod.config.LabyModConfiguration;
import org.jetbrains.annotations.NotNull;

public class NodeLabyModListener {

  private final NodeLabyModManagement labyModManagement;

  public NodeLabyModListener(@NotNull NodeLabyModManagement labyModManagement) {
    this.labyModManagement = labyModManagement;
  }

  @EventListener
  public void handleConfigUpdate(ChannelMessageReceiveEvent event) {
    if (!event.channel().equals(LabyModManagement.LABYMOD_MODULE_CHANNEL)) {
      return;
    }

    if (LabyModManagement.LABYMOD_UPDATE_CONFIG.equals(event.message())) {
      // read the configuration from the databuf
      var configuration = event.content().readObject(LabyModConfiguration.class);
      // write the configuration silently to the file
      this.labyModManagement.configurationSilently(configuration);
    }
  }

  @EventListener
  public void handle(@NotNull CloudServicePreProcessStartEvent event) {
    var service = event.getService();
    if (!ServiceEnvironmentType.isMinecraftProxy(service.getServiceId().environment())) {
      return;
    }

    var pluginsFolder = event.getService().getDirectory().resolve("plugins");
    FileUtils.createDirectory(pluginsFolder);

    var targetFile = pluginsFolder.resolve("cloudnet-labymod.jar");
    FileUtils.delete(targetFile);

    if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(NodeLabyModListener.class, targetFile)) {
      DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
        NodeLabyModListener.class,
        event.getService().getServiceId().environment(),
        targetFile
      );
    }
  }
}
