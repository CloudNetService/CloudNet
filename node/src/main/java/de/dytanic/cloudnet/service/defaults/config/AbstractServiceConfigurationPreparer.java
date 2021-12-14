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

package de.dytanic.cloudnet.service.defaults.config;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ServiceConfigurationPreparer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractServiceConfigurationPreparer implements ServiceConfigurationPreparer {

  protected static final Logger LOGGER = LogManager.getLogger(ServiceConfigurationPreparer.class);

  protected boolean shouldRewriteIp(@NotNull CloudNet nodeInstance, @NotNull ICloudService service) {
    var task = nodeInstance.getServiceTaskProvider().getServiceTask(service.getServiceId().getTaskName());
    return task == null || !task.isDisableIpRewrite();
  }

  protected void copyCompiledFile(@NotNull String fileName, @NotNull Path targetLocation) {
    if (Files.notExists(targetLocation)) {
      try (var stream = ServiceConfigurationPreparer.class.getClassLoader().getResourceAsStream(fileName)) {
        if (stream != null) {
          FileUtils.createDirectory(targetLocation.getParent());
          Files.copy(stream, targetLocation);
        }
      } catch (IOException exception) {
        LOGGER.severe("Unable to copy compiled file %s to %s:", exception, fileName, targetLocation);
      }
    }
  }

  protected void rewriteFile(@NotNull Path filePath, @NotNull UnaryOperator<String> mapper) {
    try {
      // collect the new lines rewritten by the given mapper
      Collection<String> newLines = Files.readAllLines(filePath)
        .stream()
        .map(mapper)
        .collect(Collectors.toList());
      // write the new content to the same file
      Files.write(
        filePath,
        newLines,
        StandardCharsets.UTF_8,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException exception) {
      LOGGER.severe("Unable to rewrite file %s:", exception, filePath);
    }
  }
}
