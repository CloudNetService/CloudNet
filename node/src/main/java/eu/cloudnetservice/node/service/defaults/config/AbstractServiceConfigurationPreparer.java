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

package eu.cloudnetservice.node.service.defaults.config;

import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.FileConfig;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.ServiceConfigurationPreparer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

public abstract class AbstractServiceConfigurationPreparer implements ServiceConfigurationPreparer {

  protected static final Logger LOGGER = LogManager.logger(ServiceConfigurationPreparer.class);

  protected final ServiceTaskProvider taskProvider;

  protected AbstractServiceConfigurationPreparer(@NonNull ServiceTaskProvider taskProvider) {
    this.taskProvider = taskProvider;
  }

  protected boolean shouldRewriteIp(@NonNull CloudService service) {
    var task = this.taskProvider.serviceTask(service.serviceId().taskName());
    return task == null || !task.disableIpRewrite();
  }

  protected void copyCompiledFile(@NonNull String fileName, @NonNull Path targetLocation) {
    if (Files.notExists(targetLocation)) {
      try (var stream = ServiceConfigurationPreparer.class.getClassLoader().getResourceAsStream(fileName)) {
        if (stream != null) {
          FileUtil.createDirectory(targetLocation.getParent());
          Files.copy(stream, targetLocation);
        }
      } catch (IOException exception) {
        LOGGER.severe("Unable to copy compiled file %s to %s:", exception, fileName, targetLocation);
      }
    }
  }

  protected @NonNull FileConfig loadConfig(
    @NonNull Path path,
    @NonNull ConfigFormat<?> format,
    @NonNull String defaultResourcePath
  ) {
    // build the config, use the resource at the given path to set the default data
    var defaultData = AbstractServiceConfigurationPreparer.class.getClassLoader().getResource(defaultResourcePath);
    var config = FileConfig.builder(path, format)
      .sync()
      .preserveInsertionOrder()
      .defaultData(defaultData)
      .build();

    // load and return the config
    config.load();
    return config;
  }
}
