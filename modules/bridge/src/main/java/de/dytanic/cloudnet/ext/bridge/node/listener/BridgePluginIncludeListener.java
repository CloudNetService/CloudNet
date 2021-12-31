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

package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreProcessStartEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import lombok.NonNull;

public final class BridgePluginIncludeListener {

  private final BridgeManagement management;

  public BridgePluginIncludeListener(@NonNull BridgeManagement management) {
    this.management = management;
  }

  @EventListener
  public void handle(@NonNull CloudServicePreProcessStartEvent event) {
    // check if we should copy the module
    if (this.management.configuration().excludedGroups().stream()
      .noneMatch(group -> event.service().serviceConfiguration().groups().contains(group))) {
      // get the target of the copy
      var plugins = event.service().directory().resolve("plugins");
      FileUtils.createDirectory(plugins);
      // remove the old bridge plugin
      var bridgePluginFile = plugins.resolve("cloudnet-bridge.jar");
      FileUtils.delete(bridgePluginFile);
      // try to copy the current bridge file
      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(BridgePluginIncludeListener.class, bridgePluginFile)) {
        // copy the plugin.yml file for the environment
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          BridgePluginIncludeListener.class,
          event.service().serviceId().environment(),
          bridgePluginFile);
      }
    }
  }
}
