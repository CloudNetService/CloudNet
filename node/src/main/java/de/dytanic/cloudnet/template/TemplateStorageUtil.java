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

package de.dytanic.cloudnet.template;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An util class to prepare created templates with needed files
 */
public final class TemplateStorageUtil {

  private TemplateStorageUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull LocalTemplateStorage getLocalTemplateStorage() {
    return (LocalTemplateStorage) CloudNet.getInstance().getLocalTemplateStorage();
  }

  public static @NotNull Path localPathInTemplate(@NotNull ServiceTemplate serviceTemplate, @NotNull String path) {
    return getLocalTemplateStorage().getTemplatePath(serviceTemplate).resolve(path).normalize();
  }

  public static boolean createAndPrepareTemplate(
    @NotNull SpecificTemplateStorage storage,
    @NotNull ServiceEnvironmentType environment
  ) throws IOException {
    Preconditions.checkNotNull(storage);
    Preconditions.checkNotNull(environment);

    if (!storage.exists()) {
      storage.create();
      storage.createDirectory("plugins");

      switch (environment) {
        case BUNGEECORD: {
          prepareProxyTemplate(storage, "config.yml", "files/bungee/config.yml");
        }
        break;
        case VELOCITY: {
          prepareProxyTemplate(storage, "velocity.toml", "files/velocity/velocity.toml");
        }
        break;
        case NUKKIT: {
          try (OutputStream out = storage.newOutputStream("server.properties");
            InputStream in = resourceStream("files/nukkit/server.properties")) {
            FileUtils.copy(in, out);
          }

          try (OutputStream out = storage.newOutputStream("nukkit.yml");
            InputStream in = resourceStream("files/nukkit/nukkit.yml")) {
            FileUtils.copy(in, out);
          }
        }
        break;
        case MINECRAFT_SERVER: {
          try (OutputStream out = storage.newOutputStream("server.properties");
            InputStream in = resourceStream("files/nms/server.properties")) {
            FileUtils.copy(in, out);
          }

          try (OutputStream out = storage.newOutputStream("bukkit.yml");
            InputStream in = resourceStream("files/nms/bukkit.yml")) {
            FileUtils.copy(in, out);
          }

          try (OutputStream out = storage.newOutputStream("spigot.yml");
            InputStream in = resourceStream("files/nms/spigot.yml")) {
            FileUtils.copy(in, out);
          }

          try (OutputStream out = storage.newOutputStream("config/sponge/sponge.conf");
            InputStream in = CloudNet.class.getClassLoader().getResourceAsStream("files/nms/sponge.conf")) {
            FileUtils.copy(in, out);
          }
        }
        break;
        case GLOWSTONE: {
          try (OutputStream out = storage.newOutputStream("config/glowstone.yml");
            InputStream in = resourceStream("files/glowstone/glowstone.yml")) {
            FileUtils.copy(in, out);
          }
        }
        break;
        default:
          break;
      }
      return true;
    } else {
      return false;
    }
  }

  private static void prepareProxyTemplate(
    @NotNull SpecificTemplateStorage storage,
    @NotNull String configPath,
    @NotNull String defaultConfigPath
  ) throws IOException {
    try (OutputStream out = storage.newOutputStream(configPath); InputStream in = resourceStream(defaultConfigPath)) {
      FileUtils.copy(in, out);
    }

    try (OutputStream out = storage.newOutputStream("server-icon.png");
      InputStream in = resourceStream("files/server-icon.png")) {
      FileUtils.copy(in, out);
    }
  }

  private static @Nullable InputStream resourceStream(@NotNull String path) {
    return TemplateStorageUtil.class.getClassLoader().getResourceAsStream(path);
  }
}
