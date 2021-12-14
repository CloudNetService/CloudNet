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

package de.dytanic.cloudnet.ext.bridge.node.listener;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreLifecycleEvent;
import de.dytanic.cloudnet.ext.bridge.BridgeManagement;
import org.jetbrains.annotations.NotNull;

public final class BridgePluginIncludeListener {

  private final BridgeManagement management;

  public BridgePluginIncludeListener(@NotNull BridgeManagement management) {
    this.management = management;
  }

  @EventListener
  public void handle(@NotNull CloudServicePreLifecycleEvent event) {
    // check if we should copy the module
    if (event.getTargetLifecycle() == ServiceLifeCycle.RUNNING
      && this.management.getConfiguration().getExcludedGroups().stream()
      .noneMatch(group -> event.getService().getServiceConfiguration().getGroups().contains(group))) {
      // get the target of the copy
      var plugins = event.getService().getDirectory().resolve("plugins");
      FileUtils.createDirectory(plugins);
      // remove the old bridge plugin
      var bridgePluginFile = plugins.resolve("cloudnet-bridge.jar");
      FileUtils.delete(bridgePluginFile);
      // try to copy the current bridge file
      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(BridgePluginIncludeListener.class, bridgePluginFile)) {
        // copy the plugin.yml file for the environment
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          BridgePluginIncludeListener.class,
          event.getService().getServiceId().getEnvironment(),
          bridgePluginFile);
      }
    }
  }
}
