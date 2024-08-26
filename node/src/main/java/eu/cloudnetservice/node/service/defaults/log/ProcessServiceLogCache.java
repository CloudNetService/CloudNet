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

package eu.cloudnetservice.node.service.defaults.log;

import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.service.CloudService;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

public class ProcessServiceLogCache extends AbstractServiceLogCache {

  public ProcessServiceLogCache(@NonNull Configuration configuration, @NonNull CloudService service) {
    super(configuration, service);
  }

  public void start(@NonNull Process process) {
    var inputStreamReader = process.inputReader(StandardCharsets.UTF_8);
    this.startStreamReadingTask(inputStreamReader, false);

    var errorStreamReader = process.errorReader(StandardCharsets.UTF_8);
    this.startStreamReadingTask(errorStreamReader, true);
  }

  protected void startStreamReadingTask(@NonNull BufferedReader reader, boolean isErrorStream) {
    var serviceName = this.service.serviceId().name();
    var streamTypeDisplayName = isErrorStream ? "error" : "output";
    var threadName = String.format("%s %s-stream reader", serviceName, streamTypeDisplayName);

    Thread.ofVirtual()
      .name(threadName)
      .inheritInheritableThreadLocals(false)
      .start(() -> {
        while (true) {
          try {
            var logLine = reader.readLine();
            if (logLine == null) {
              // reached EOF, process terminated
              break;
            }

            this.handleItem(logLine, isErrorStream);
          } catch (IOException exception) {
            LOGGER.error("Exception reading {} stream of service {}", streamTypeDisplayName, serviceName, exception);
          }
        }
      });
  }
}
