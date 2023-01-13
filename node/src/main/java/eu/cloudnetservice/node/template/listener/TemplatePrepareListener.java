/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node.template.listener;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.node.event.template.ServiceTemplateInstallEvent;
import eu.cloudnetservice.node.template.TemplateStorageUtil;
import java.io.IOException;
import java.io.InputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class TemplatePrepareListener {

  @EventListener
  public void handle(@NonNull ServiceTemplateInstallEvent event) throws IOException {
    if (event.environmentType().equals(ServiceEnvironmentType.BUNGEECORD)) {
      // config.yml & server icon
      this.prepareProxyTemplate(event.storage(), event.template(), "config.yml", "files/bungee/config.yml");
    } else if (event.environmentType().equals(ServiceEnvironmentType.VELOCITY)) {
      // velocity.toml & server icon
      this.prepareProxyTemplate(event.storage(), event.template(), "velocity.toml", "files/velocity/velocity.toml");
    } else if (event.environmentType().equals(ServiceEnvironmentType.NUKKIT)) {
      // server.properties & nukkit.yml
      try (var out = event.storage().newOutputStream(event.template(), "server.properties");
        var in = this.resourceStream("files/nukkit/server.properties")) {
        FileUtil.copy(in, out);
      }

      try (var out = event.storage().newOutputStream(event.template(), "nukkit.yml");
        var in = this.resourceStream("files/nukkit/nukkit.yml")) {
        FileUtil.copy(in, out);
      }
    } else if (event.environmentType().equals(ServiceEnvironmentType.MINECRAFT_SERVER)) {
      // server.properties, bukkit.yml, spigot.yml & sponge.conf
      try (var out = event.storage().newOutputStream(event.template(), "server.properties");
        var in = this.resourceStream("files/nms/server.properties")) {
        FileUtil.copy(in, out);
      }

      try (var out = event.storage().newOutputStream(event.template(), "bukkit.yml");
        var in = this.resourceStream("files/nms/bukkit.yml")) {
        FileUtil.copy(in, out);
      }

      try (var out = event.storage().newOutputStream(event.template(), "spigot.yml");
        var in = this.resourceStream("files/nms/spigot.yml")) {
        FileUtil.copy(in, out);
      }

      try (var out = event.storage().newOutputStream(event.template(), "config/sponge/sponge.conf");
        var in = this.resourceStream("files/nms/sponge.conf")) {
        FileUtil.copy(in, out);
      }
    } else if (event.environmentType().equals(ServiceEnvironmentType.MODDED_MINECRAFT_SERVER)) {
      try (var out = event.storage().newOutputStream(event.template(), "server.properties")) {
        var in = this.resourceStream("files/nms/server.properties");
        FileUtil.copy(in, out);
      }
    }
  }

  private void prepareProxyTemplate(
    @NonNull TemplateStorage storage,
    @NonNull ServiceTemplate template,
    @NonNull String target,
    @NonNull String internalPath
  ) throws IOException {
    try (var out = storage.newOutputStream(template, target); var in = this.resourceStream(internalPath)) {
      FileUtil.copy(in, out);
    }

    try (var out = storage.newOutputStream(template, "server-icon.png");
      var in = this.resourceStream("files/server-icon.png")) {
      FileUtil.copy(in, out);
    }
  }

  private @Nullable InputStream resourceStream(@NonNull String path) {
    return TemplateStorageUtil.class.getClassLoader().getResourceAsStream(path);
  }
}
