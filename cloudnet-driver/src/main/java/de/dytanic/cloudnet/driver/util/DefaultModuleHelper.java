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

package de.dytanic.cloudnet.driver.util;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.unsafe.ResourceResolver;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class is for utility methods for the base modules in this multi module project
 */
public final class DefaultModuleHelper {

  public static final String DEFAULT_CONFIGURATION_DATABASE_NAME = "cloudNet_module_configuration";

  private DefaultModuleHelper() {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @ApiStatus.ScheduledForRemoval
  public static boolean copyCurrentModuleInstanceFromClass(Class<?> clazz, File target) {
    return copyCurrentModuleInstanceFromClass(clazz, target.toPath());
  }

  public static boolean copyCurrentModuleInstanceFromClass(Class<?> clazz, Path target) {
    Preconditions.checkNotNull(clazz);
    Preconditions.checkNotNull(target);

    try {
      URI uri = ResourceResolver.resolveURIFromResourceByClass(clazz);
      if (uri != null) {
        URLConnection connection = uri.toURL().openConnection();
        connection.setRequestProperty("User-Agent",
          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        connection.connect();

        try (InputStream inputStream = connection.getInputStream(); OutputStream outputStream = Files
          .newOutputStream(target)) {
          FileUtils.copy(inputStream, outputStream);
        }

        return true;
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    return false;
  }

  public static void copyPluginConfigurationFileForEnvironment(Class<?> targetClass, ServiceEnvironmentType type,
    Path file) {
    FileUtils.openZipFileSystem(file, fileSystem -> {
      Path pluginPath = fileSystem.getPath("plugin.yml");

      if (Files.exists(pluginPath)) {
        Files.delete(pluginPath);
      }

      switch (type) {
        case VELOCITY:
          break;
        case BUNGEECORD:
          try (InputStream inputStream = targetClass.getClassLoader().getResourceAsStream("plugin.bungee.yml")) {
            if (inputStream != null) {
              Files.copy(inputStream, pluginPath);
            }
          }
          break;
        case WATERDOG_PE:
          try (InputStream inputStream = targetClass.getClassLoader().getResourceAsStream("plugin.waterdogpe.yml")) {
            if (inputStream != null) {
              Files.copy(inputStream, pluginPath);
            }
          }
          break;
        case NUKKIT:
          try (InputStream inputStream = targetClass.getClassLoader().getResourceAsStream("plugin.nukkit.yml")) {
            if (inputStream != null) {
              Files.copy(inputStream, pluginPath);
            }
          }
          break;
        default:
          try (InputStream inputStream = targetClass.getClassLoader().getResourceAsStream("plugin.bukkit.yml")) {
            if (inputStream != null) {
              Files.copy(inputStream, pluginPath);
            }
          }
          break;
      }
      return null;
    });
  }
}
