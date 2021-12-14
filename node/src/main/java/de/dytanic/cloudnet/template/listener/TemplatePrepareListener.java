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

package de.dytanic.cloudnet.template.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.event.template.ServiceTemplateInstallEvent;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TemplatePrepareListener {

  @EventListener
  public void handle(@NotNull ServiceTemplateInstallEvent event) throws IOException {
    if (event.getEnvironmentType().equals(ServiceEnvironmentType.BUNGEECORD)) {
      // config.yml & server icon
      this.prepareProxyTemplate(event.getStorage(), "config.yml", "files/bungee/config.yml");
    } else if (event.getEnvironmentType().equals(ServiceEnvironmentType.VELOCITY)) {
      // velocity.toml & server icon
      this.prepareProxyTemplate(event.getStorage(), "velocity.toml", "files/velocity/velocity.toml");
    } else if (event.getEnvironmentType().equals(ServiceEnvironmentType.NUKKIT)) {
      // server.properties & nukkit.yml
      try (var out = event.getStorage().newOutputStream("server.properties");
        var in = resourceStream("files/nukkit/server.properties")) {
        FileUtils.copy(in, out);
      }

      try (var out = event.getStorage().newOutputStream("nukkit.yml");
        var in = resourceStream("files/nukkit/nukkit.yml")) {
        FileUtils.copy(in, out);
      }
    } else if (event.getEnvironmentType().equals(ServiceEnvironmentType.MINECRAFT_SERVER)) {
      // server.properties, bukkit.yml, spigot.yml & sponge.conf
      try (var out = event.getStorage().newOutputStream("server.properties");
        var in = resourceStream("files/nms/server.properties")) {
        FileUtils.copy(in, out);
      }

      try (var out = event.getStorage().newOutputStream("bukkit.yml");
        var in = resourceStream("files/nms/bukkit.yml")) {
        FileUtils.copy(in, out);
      }

      try (var out = event.getStorage().newOutputStream("spigot.yml");
        var in = resourceStream("files/nms/spigot.yml")) {
        FileUtils.copy(in, out);
      }

      try (var out = event.getStorage().newOutputStream("config/sponge/sponge.conf");
        var in = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/sponge.conf")) {
        FileUtils.copy(in, out);
      }
    } else if (event.getEnvironmentType().equals(ServiceEnvironmentType.GLOWSTONE)) {
      // glowstone.yml
      try (var out = event.getStorage().newOutputStream("config/glowstone.yml");
        var in = resourceStream("files/glowstone/glowstone.yml")) {
        FileUtils.copy(in, out);
      }
    }
  }

  private void prepareProxyTemplate(
    @NotNull SpecificTemplateStorage storage,
    @NotNull String target,
    @NotNull String internalPath
  ) throws IOException {
    try (var out = storage.newOutputStream(target); var in = this.resourceStream(internalPath)) {
      FileUtils.copy(in, out);
    }

    try (var out = storage.newOutputStream("server-icon.png");
      var in = resourceStream("files/server-icon.png")) {
      FileUtils.copy(in, out);
    }
  }

  private @Nullable InputStream resourceStream(@NotNull String path) {
    return TemplateStorageUtil.class.getClassLoader().getResourceAsStream(path);
  }
}
