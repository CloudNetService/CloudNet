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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SyncProxyConfigurationWriterAndReader {

  private static final Map<String, String> DEFAULT_MESSAGES = new HashMap<>(ImmutableMap.of(
    "player-login-not-whitelisted", "&cThe network is currently in maintenance!",
    "player-login-full-server", "&cThe network is currently full. You need extra permissions to enter the network",
    "service-start", "&7The service &e%service% &7is &astarting &7on node &e%node%&7...",
    "service-stop", "&7The service &e%service% &7is &cstopping &7on node &e%node%&7..."
  ));

  private SyncProxyConfigurationWriterAndReader() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  public static void write(SyncProxyConfiguration syncProxyConfiguration, File file) {
    write(syncProxyConfiguration, file.toPath());
  }

  public static void write(SyncProxyConfiguration syncProxyConfiguration, Path file) {
    Preconditions.checkNotNull(syncProxyConfiguration);
    Preconditions.checkNotNull(file);

    JsonDocument.newDocument("config", syncProxyConfiguration).write(file);
  }

  @Deprecated
  public static SyncProxyConfiguration read(File file) {
    return read(file.toPath());
  }

  public static SyncProxyConfiguration read(Path file) {
    Preconditions.checkNotNull(file);

    JsonDocument document = JsonDocument.newDocument(file);
    if (!document.contains("config")) {
      write(new SyncProxyConfiguration(
        new ArrayList<>(),
        new ArrayList<>(),
        DEFAULT_MESSAGES,
        true
      ), file);
      document = JsonDocument.newDocument(file);
    }

    SyncProxyConfiguration configuration = document.get("config", SyncProxyConfiguration.TYPE);
    if (configuration.getMessages() != null) {
      boolean edit = false;
      for (Map.Entry<String, String> entry : DEFAULT_MESSAGES.entrySet()) {
        if (!configuration.getMessages().containsKey(entry.getKey())) {
          configuration.getMessages().put(entry.getKey(), entry.getValue());
          edit = true;
        }
      }
      if (edit) {
        write(configuration, file);
      }
    } else {
      configuration.setMessages(new HashMap<>(DEFAULT_MESSAGES));
      write(configuration, file);
    }
    return configuration;
  }

  public static SyncProxyTabListConfiguration createDefaultTabListConfiguration(String targetGroup) {
    return new SyncProxyTabListConfiguration(
      targetGroup,
      Collections.singletonList(
        new SyncProxyTabList(
          " \n &b&o■ &8┃ &3&lCloudNet &8● &7Earthquake &8&l» &7&o%online_players%&8/&7&o%max_players% &8┃ &b&o■ "
            + "\n &8► &7Current server &8● &b%server% &8◄ \n ",
          " \n &7Discord &8&l» &bdiscord.cloudnetservice.eu \n &7&onext &3&l&ogeneration &7&onetwork \n "
        )
      ),
      1
    );
  }

  public static SyncProxyProxyLoginConfiguration createDefaultLoginConfiguration(String targetGroup) {
    return new SyncProxyProxyLoginConfiguration(
      targetGroup,
      false,
      100,
      new ArrayList<>(),
      Collections.singletonList(new SyncProxyMotd(
        "         &b&o■ &8┃ &3&lCloudNet &8● &7Earthquake &8&l» &7&ov3.4 &8┃ &b&o■",
        "              &7&onext &3&l&ogeneration &7&onetwork",
        false,
        1,
        new String[]{},
        null
      )),
      Collections.singletonList(new SyncProxyMotd(
        "         &b&o■ &8┃ &3&lCloudNet &8● &7Earthquake &8&l» &7&ov3.4 &8┃ &b&o■",
        "     &3&lMaintenance &8&l» &7We are still in &3&lmaintenance",
        false,
        1,
        new String[]{
          " ",
          "&b&o■ &8┃ &3&lCloudNet &8● &7Earthquake &8&l» &7&ov3.4 &8┃ &b&o■",
          "&7Discord &8&l» &bdiscord.cloudnetservice.eu",
          " "
        },
        "&8➜ &c&lMaintenance &8┃ &c✘"
      ))
    );
  }

}
