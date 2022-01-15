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

package eu.cloudnetservice.modules.cloudperms.node.listener;

import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.util.DefaultModuleHelper;
import eu.cloudnetservice.cloudnet.node.event.service.CloudServicePreProcessStartEvent;
import eu.cloudnetservice.modules.cloudperms.node.CloudNetCloudPermissionsModule;
import lombok.NonNull;

public final class IncludePluginListener {

  private final CloudNetCloudPermissionsModule permissionsModule;

  public IncludePluginListener(CloudNetCloudPermissionsModule permissionsModule) {
    this.permissionsModule = permissionsModule;
  }

  @EventListener
  public void handle(@NonNull CloudServicePreProcessStartEvent event) {
    // check if we should copy the module
    if (this.permissionsModule.permissionsConfig().enabled()
      && this.permissionsModule.permissionsConfig().excludedGroups().stream()
      .noneMatch(group -> event.service().serviceConfiguration().groups().contains(group))) {
      // get the target of the copy
      var plugins = event.service().directory().resolve("plugins");
      FileUtil.createDirectory(plugins);
      // remove the old perms plugin
      var permsPluginFile = plugins.resolve("cloudnet-cloudperms.jar");
      FileUtil.delete(permsPluginFile);
      // try to copy the current perms file
      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, permsPluginFile)) {
        // copy the plugin.yml file for the environment
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          IncludePluginListener.class,
          event.service().serviceId().environment(),
          permsPluginFile);
      }
    }
  }
}
