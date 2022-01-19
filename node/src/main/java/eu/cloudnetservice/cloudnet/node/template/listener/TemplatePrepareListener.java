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

package eu.cloudnetservice.cloudnet.node.template.listener;

import eu.cloudnetservice.cloudnet.common.io.FileUtil;
import eu.cloudnetservice.cloudnet.driver.event.EventListener;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.driver.template.SpecificTemplateStorage;
import eu.cloudnetservice.cloudnet.node.event.template.ServiceTemplateInstallEvent;
import eu.cloudnetservice.cloudnet.node.template.TemplateStorageUtil;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class TemplatePrepareListener {

  @EventListener
  public void handle(@NonNull ServiceTemplateInstallEvent event) throws IOException {
    if (event.environmentType().equals(ServiceEnvironmentType.BUNGEECORD)) {
      // config.yml & server icon
      this.prepareProxyTemplate(event.storage(), "config.yml", "files/bungee/config.yml");
    } else if (event.environmentType().equals(ServiceEnvironmentType.VELOCITY)) {
      // velocity.toml & server icon
      this.prepareProxyTemplate(event.storage(), "velocity.toml", "files/velocity/velocity.toml");
    } else if (event.environmentType().equals(ServiceEnvironmentType.NUKKIT)) {
      // server.properties & nukkit.yml
      try (var out = event.storage().newOutputStream("server.properties");
        var in = resourceStream("files/nukkit/server.properties")) {
        FileUtil.copy(in, out);
      }

      try (var out = event.storage().newOutputStream("nukkit.yml");
        var in = resourceStream("files/nukkit/nukkit.yml")) {
        FileUtil.copy(in, out);
      }
    } else if (event.environmentType().equals(ServiceEnvironmentType.MINECRAFT_SERVER)) {
      // server.properties, bukkit.yml, spigot.yml & sponge.conf
      try (var out = event.storage().newOutputStream("server.properties");
        var in = resourceStream("files/nms/server.properties")) {
        FileUtil.copy(in, out);
      }

      try (var out = event.storage().newOutputStream("bukkit.yml");
        var in = resourceStream("files/nms/bukkit.yml")) {
        FileUtil.copy(in, out);
      }

      try (var out = event.storage().newOutputStream("spigot.yml");
        var in = resourceStream("files/nms/spigot.yml")) {
        FileUtil.copy(in, out);
      }

      try (var out = event.storage().newOutputStream("config/sponge/sponge.conf");
        var in = resourceStream("files/nms/sponge.conf")) {
        FileUtil.copy(in, out);
      }
    } else if (event.environmentType().equals(ServiceEnvironmentType.MODDED_MINECRAFT_SERVER)) {
      try (var out = event.storage().newOutputStream("server.properties")) {
        var in = resourceStream("files/fabric/server.properties");
        FileUtil.copy(in, out);
      }
    } else if (event.environmentType().equals(ServiceEnvironmentType.GLOWSTONE)) {
      // glowstone.yml
      try (var out = event.storage().newOutputStream("config/glowstone.yml");
        var in = resourceStream("files/glowstone/glowstone.yml")) {
        FileUtil.copy(in, out);
      }
    }
  }

  private void prepareProxyTemplate(
    @NonNull SpecificTemplateStorage storage,
    @NonNull String target,
    @NonNull String internalPath
  ) throws IOException {
    try (var out = storage.newOutputStream(target); var in = this.resourceStream(internalPath)) {
      FileUtil.copy(in, out);
    }

    try (var out = storage.newOutputStream("server-icon.png");
      var in = resourceStream("files/server-icon.png")) {
      FileUtil.copy(in, out);
    }
  }

  private @Nullable InputStream resourceStream(@NonNull String path) {
    return TemplateStorageUtil.class.getClassLoader().getResourceAsStream(path);
  }
}
