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

package eu.cloudnetservice.cloudnet.node.service.defaults.config;

import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.service.CloudService;
import eu.cloudnetservice.cloudnet.node.service.ServiceConfigurationPreparer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.function.UnaryOperator;
import lombok.NonNull;

public abstract class AbstractServiceConfigurationPreparer implements ServiceConfigurationPreparer {

  protected static final Logger LOGGER = LogManager.logger(ServiceConfigurationPreparer.class);

  protected boolean shouldRewriteIp(@NonNull CloudNet nodeInstance, @NonNull CloudService service) {
    var task = nodeInstance.serviceTaskProvider().serviceTask(service.serviceId().taskName());
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

  protected void rewriteFile(@NonNull Path filePath, @NonNull UnaryOperator<String> mapper) {
    try {
      // collect the new lines rewritten by the given mapper
      Collection<String> newLines = Files.readAllLines(filePath)
        .stream()
        .map(mapper)
        .toList();
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
