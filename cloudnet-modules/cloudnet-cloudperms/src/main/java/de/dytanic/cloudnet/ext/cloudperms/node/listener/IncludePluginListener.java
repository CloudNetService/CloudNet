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
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import de.dytanic.cloudnet.ext.cloudperms.node.CloudNetCloudPermissionsModule;
import java.nio.file.Path;
import java.util.Arrays;

public final class IncludePluginListener {

  @EventListener
  public void handle(CloudServicePreStartEvent event) {
    boolean installPlugin =
      CloudNetCloudPermissionsModule.getInstance().getConfig().getBoolean("enabled") && CloudNetCloudPermissionsModule
        .getInstance().getExcludedGroups()
        .stream()
        .noneMatch(excludedGroup -> Arrays.asList(event.getCloudService().getServiceConfiguration().getGroups())
          .contains(excludedGroup));

    Path pluginsFolder = event.getCloudService().getDirectoryPath().resolve("plugins");
    FileUtils.createDirectoryReported(pluginsFolder);

    Path targetFile = pluginsFolder.resolve("cloudnet-cloudperms.jar");
    FileUtils.deleteFileReported(targetFile);

    if (installPlugin && DefaultModuleHelper
      .copyCurrentModuleInstanceFromClass(IncludePluginListener.class, targetFile)) {
      DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
        IncludePluginListener.class,
        event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(),
        targetFile
      );
    }
  }
}
