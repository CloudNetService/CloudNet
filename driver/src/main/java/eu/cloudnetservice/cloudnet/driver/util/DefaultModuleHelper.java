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

package eu.cloudnetservice.cloudnet.driver.util;

import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.common.unsafe.ResourceResolver;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * This module helper class provides helper methods for cloudnet modules that also operate as a plugin on the services.
 *
 * @author Aldin S. (0utplay@cloudnetservice.eu)
 * @author Pasqual Koschmieder. (derklaro@cloudnetservice.eu)
 * @since 4.0
 */
public final class DefaultModuleHelper {

  private static final Logger LOGGER = LogManager.logger(DefaultModuleHelper.class);

  /**
   * Creating an instance of this helper class is not allowed, results in {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException on invocation
   */
  private DefaultModuleHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Copies the caller {@code clazz} location from the class path to the given {@code target} path.
   *
   * @param clazz  the class of which the associated class path entry should get copied.
   * @param target the target file to copy the file into - the file is not required to exist.
   * @return true if the entry was copied successfully, false otherwise.
   * @throws NullPointerException if {@code clazz} or {@code target} is null.
   */
  public static boolean copyCurrentModuleInstanceFromClass(@NonNull Class<?> clazz, @NonNull Path target) {
    try {
      // get the location of the class path entry associated with the given class
      var uri = ResourceResolver.resolveURIFromResourceByClass(clazz);
      // copy the file
      try (var out = Files.newOutputStream(target)) {
        FileUtils.copy(uri.toURL().openStream(), out);
        return true;
      }
    } catch (IOException exception) {
      LOGGER.severe("Unable to copy class path entry of " + clazz + " to " + target, exception);
      return false;
    }
  }

  /**
   * Copies the appropriate plugin configuration file for the given service environment {@code type}. All the listed
   * files will be copied and renamed to {@code plugin.yml}:
   *
   * <ul>
   *   <li>plugin.bungee.yml if the given environment is BUNGEECORD.</li>
   *   <li>plugin.waterdogpe.yml if the given environment is WATERDOG_PE.</li>
   *   <li>plugin.nukkit.yml if the given environment is NUKKIT.</li>
   *   <li>plugin.bukkit.yml is used as a fallback - if the file does not exist it will not be used.</li>
   * </ul>
   *
   * @param clazz the class of which the associated class path entry should get copied.
   * @param type  the current {@link ServiceEnvironmentType} of the service the file gets copied for.
   * @param file  the target file of the plugin to copy the file for.
   * @throws NullPointerException if clazz, type or file is null.
   */
  public static void copyPluginConfigurationFileForEnvironment(
    @NonNull Class<?> clazz,
    @NonNull ServiceEnvironmentType type,
    @NonNull Path file
  ) {
    FileUtils.openZipFileSystem(file, fileSystem -> {
      // check if there is a plugin.yml file already - delete if it exists
      var pluginPath = fileSystem.getPath("plugin.yml");
      if (Files.exists(pluginPath)) {
        Files.delete(pluginPath);
      }
      // select the input stream to copy the file from
      var in = clazz.getClassLoader().getResourceAsStream(String.format(
        "plugin.%s.yml",
        type.name().toLowerCase()));
      // copy the file if the file exists
      if (in != null) {
        Files.copy(in, pluginPath);
        // close the stream to avoid resource leaks
        in.close();
      }
    });
  }
}
