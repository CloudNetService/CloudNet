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

package eu.cloudnetservice.modules.signs.node.util;

import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.util.DefaultModuleHelper;
import eu.cloudnetservice.cloudnet.node.service.CloudService;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class SignPluginInclusion {

  private SignPluginInclusion() {
    throw new UnsupportedOperationException();
  }

  public static void includePluginTo(@NonNull CloudService cloudService, @NonNull SignsConfiguration configuration) {
    var type = cloudService.serviceConfiguration().serviceId().environment();
    if (ServiceEnvironmentType.isMinecraftServer(type)
      && hasConfigurationEntry(cloudService.serviceConfiguration().groups(), configuration)) {
      var pluginDirectory = cloudService.directory().resolve("plugins");
      FileUtils.createDirectory(pluginDirectory);

      var pluginFile = pluginDirectory.resolve("cloudnet-signs.jar");
      FileUtils.delete(pluginFile);

      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(SignPluginInclusion.class, pluginFile)) {
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(SignPluginInclusion.class, type, pluginFile);
      }
    }
  }

  public static boolean hasConfigurationEntry(@NonNull Collection<String> groups, @NonNull SignsConfiguration config) {
    for (var entry : config.configurationEntries()) {
      if (groups.contains(entry.targetGroup())) {
        return true;
      }
    }
    return false;
  }
}
