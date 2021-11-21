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
import java.io.OutputStream;
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
      try (OutputStream out = event.getStorage().newOutputStream("server.properties");
        InputStream in = resourceStream("files/nukkit/server.properties")) {
        FileUtils.copy(in, out);
      }

      try (OutputStream out = event.getStorage().newOutputStream("nukkit.yml");
        InputStream in = resourceStream("files/nukkit/nukkit.yml")) {
        FileUtils.copy(in, out);
      }
    } else if (event.getEnvironmentType().equals(ServiceEnvironmentType.MINECRAFT_SERVER)) {
      // server.properties, bukkit.yml, spigot.yml & sponge.conf
      try (OutputStream out = event.getStorage().newOutputStream("server.properties");
        InputStream in = resourceStream("files/nms/server.properties")) {
        FileUtils.copy(in, out);
      }

      try (OutputStream out = event.getStorage().newOutputStream("bukkit.yml");
        InputStream in = resourceStream("files/nms/bukkit.yml")) {
        FileUtils.copy(in, out);
      }

      try (OutputStream out = event.getStorage().newOutputStream("spigot.yml");
        InputStream in = resourceStream("files/nms/spigot.yml")) {
        FileUtils.copy(in, out);
      }

      try (OutputStream out = event.getStorage().newOutputStream("config/sponge/sponge.conf");
        InputStream in = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/sponge.conf")) {
        FileUtils.copy(in, out);
      }
    } else if (event.getEnvironmentType().equals(ServiceEnvironmentType.GLOWSTONE)) {
      // glowstone.yml
      try (OutputStream out = event.getStorage().newOutputStream("config/glowstone.yml");
        InputStream in = resourceStream("files/glowstone/glowstone.yml")) {
        FileUtils.copy(in, out);
      }
    }
  }

  private void prepareProxyTemplate(
    @NotNull SpecificTemplateStorage storage,
    @NotNull String target,
    @NotNull String internalPath
  ) throws IOException {
    try (OutputStream out = storage.newOutputStream(target); InputStream in = this.resourceStream(internalPath)) {
      FileUtils.copy(in, out);
    }

    try (OutputStream out = storage.newOutputStream("server-icon.png");
      InputStream in = resourceStream("files/server-icon.png")) {
      FileUtils.copy(in, out);
    }
  }

  private @Nullable InputStream resourceStream(@NotNull String path) {
    return TemplateStorageUtil.class.getClassLoader().getResourceAsStream(path);
  }
}
