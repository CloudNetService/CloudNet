/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.module.listener;

import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.node.event.service.CloudServicePreProcessStartEvent;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.utils.base.io.FileUtil;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PluginIncludeListener(
  @NonNull String moduleName,
  @NonNull Class<?> moduleClass,
  @NonNull ModuleHelper moduleHelper,
  @NonNull Function<CloudService, Boolean> includeChecker,
  @Nullable BiConsumer<CloudService, Path> includeHandler
) {

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginIncludeListener.class);

  public PluginIncludeListener(
    @NonNull String moduleName,
    @NonNull Class<?> moduleClass,
    @NonNull ModuleHelper moduleHelper,
    @NonNull Function<CloudService, Boolean> includeChecker
  ) {
    this(moduleName, moduleClass, moduleHelper, includeChecker, null);
  }

  @EventListener
  public void handle(@NonNull CloudServicePreProcessStartEvent event) {
    if (this.includeChecker.apply(event.service())) {
      LOGGER.debug("Including the module {} to service {}", this.moduleName, event.service().serviceId());
      // remove the old plugin file if it exists
      var pluginFile = event.service().pluginDirectory().resolve(this.moduleName + ".jar");
      FileUtil.delete(pluginFile);
      // try to copy the current plugin file
      if (this.moduleHelper.copyJarContainingClass(this.moduleClass, pluginFile)) {
        // copy the plugin.yml file for the environment
        this.moduleHelper.copyPluginConfigurationFileForEnvironment(
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
