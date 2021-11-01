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

package de.dytanic.cloudnet.ext.syncproxy.configuration;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public final class SyncProxyConfigurationHelper {

  private SyncProxyConfigurationHelper() {
    throw new UnsupportedOperationException();
  }

  public static void write(@NotNull SyncProxyConfiguration syncProxyConfiguration, @NotNull Path location) {
    JsonDocument.newDocument("config", syncProxyConfiguration).write(location);
  }

  public static SyncProxyConfiguration read(@NotNull Path location) {
    JsonDocument document = JsonDocument.newDocument(location);
    if (!document.contains("config")) {
      SyncProxyConfiguration configuration = SyncProxyConfiguration.createDefault("Proxy");
      write(configuration, location);
      document = JsonDocument.newDocument("config", configuration);
    }

    SyncProxyConfiguration configuration = document.get("config", SyncProxyConfiguration.class);
    if (configuration.getMessages() != null) {
      boolean edit = false;
      for (Map.Entry<String, String> entry : SyncProxyConfiguration.DEFAULT_MESSAGES.entrySet()) {
        if (!configuration.getMessages().containsKey(entry.getKey())) {
          configuration.getMessages().put(entry.getKey(), entry.getValue());
          edit = true;
        }
      }
      if (edit) {
        write(configuration, location);
      }
    } else {
      configuration.setMessages(new HashMap<>(SyncProxyConfiguration.DEFAULT_MESSAGES));
      write(configuration, location);
    }
    return configuration;
  }


}
