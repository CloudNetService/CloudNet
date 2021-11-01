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

public final class IncludePluginListener {

  @EventListener
  public void handle(CloudServicePreLifecycleEvent event) {
    if (event.getTargetLifecycle() != ServiceLifeCycle.RUNNING) {
      return;
    }

    boolean installPlugin =
      CloudNetCloudPermissionsModule.getInstance().getConfig().getBoolean("enabled") && CloudNetCloudPermissionsModule
        .getInstance().getExcludedGroups()
        .stream()
        .noneMatch(excludedGroup -> event.getService().getServiceConfiguration().getGroups().contains(excludedGroup));

    Path pluginsFolder = event.getService().getDirectory().resolve("plugins");
    FileUtils.createDirectory(pluginsFolder);

    Path targetFile = pluginsFolder.resolve("cloudnet-cloudperms.jar");
    FileUtils.delete(targetFile);

    if (installPlugin && DefaultModuleHelper
      .copyCurrentModuleInstanceFromClass(IncludePluginListener.class, targetFile)) {
      DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
        IncludePluginListener.class,
        event.getService().getServiceConfiguration().getProcessConfig().getEnvironment(),
        targetFile
      );
    }
  }
}
