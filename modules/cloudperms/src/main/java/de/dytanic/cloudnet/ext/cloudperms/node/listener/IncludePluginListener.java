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

package de.dytanic.cloudnet.ext.cloudperms.node.listener;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreLifecycleEvent;
import de.dytanic.cloudnet.ext.cloudperms.node.CloudNetCloudPermissionsModule;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public final class IncludePluginListener {

  private final CloudNetCloudPermissionsModule permissionsModule;

  public IncludePluginListener(CloudNetCloudPermissionsModule permissionsModule) {
    this.permissionsModule = permissionsModule;
  }

  @EventListener
  public void handle(@NotNull CloudServicePreLifecycleEvent event) {
    // check if we should copy the module
    if (event.getTargetLifecycle() == ServiceLifeCycle.RUNNING
      && this.permissionsModule.getPermissionsConfig().isEnabled()
      && this.permissionsModule.getPermissionsConfig().getExcludedGroups().stream()
      .noneMatch(group -> event.getService().getServiceConfiguration().getGroups().contains(group))) {
      // get the target of the copy
      var plugins = event.getService().getDirectory().resolve("plugins");
      FileUtils.createDirectory(plugins);
      // remove the old perms plugin
      var permsPluginFile = plugins.resolve("cloudnet-cloudperms.jar");
      FileUtils.delete(permsPluginFile);
      // try to copy the current perms file
      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, permsPluginFile)) {
        // copy the plugin.yml file for the environment
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          IncludePluginListener.class,
          event.getService().getServiceId().getEnvironment(),
          permsPluginFile);
      }
    }
  }
}
