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

package eu.cloudnetservice.node.module.listener;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.util.DefaultModuleHelper;
import eu.cloudnetservice.node.event.service.CloudServicePreProcessStartEvent;
import eu.cloudnetservice.node.service.CloudService;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record PluginIncludeListener(
  @NonNull String moduleName,
  @NonNull Class<?> moduleClass,
  @NonNull Function<CloudService, Boolean> includeChecker,
  @Nullable BiConsumer<CloudService, Path> includeHandler
) {

  private static final Logger LOGGER = LogManager.logger(PluginIncludeListener.class);

  public PluginIncludeListener(
    @NonNull String moduleName,
    @NonNull Class<?> moduleClass,
    @NonNull Function<CloudService, Boolean> includeChecker
  ) {
    this(moduleName, moduleClass, includeChecker, null);
  }

  @EventListener
  public void handle(@NonNull CloudServicePreProcessStartEvent event) {
    if (this.includeChecker.apply(event.service())) {
      LOGGER.fine("Including the module %s to service %s", null, this.moduleName, event.service().serviceId());
      // remove the old plugin file if it exists
      var pluginFile = event.service().pluginDirectory().resolve(this.moduleName + ".jar");
      FileUtil.delete(pluginFile);
      // try to copy the current plugin file
      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(this.moduleClass, pluginFile)) {
        // copy the plugin.yml file for the environment
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          this.moduleClass,
          event.service().serviceId().environment(),
          pluginFile);
        // check if a post listener is available
        if (this.includeHandler != null) {
          this.includeHandler.accept(event.service(), pluginFile);
        }
      }
    }
  }
}
