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

package eu.cloudnetservice.driver.impl.module;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.network.object.DefaultObjectMapper;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.handler.RPCHandlerRegistry;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.utils.base.StringUtil;
import eu.cloudnetservice.utils.base.io.FileUtil;
import eu.cloudnetservice.utils.base.resource.ResourceResolver;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module helper class provides helper methods for cloudnet modules that also operate as a plugin on the services.
 *
 * @since 4.0
 */
@Singleton
@Deprecated
public final class ModuleHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModuleHelper.class);

  private final EventManager eventManager;
  private final NetworkClient networkClient;
  private final ServiceRegistry serviceRegistry;
  private final RPCHandlerRegistry rpcHandlerRegistry;

  @Inject
  public ModuleHelper(
    @NonNull EventManager eventManager,
    @NonNull NetworkClient networkClient,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull RPCHandlerRegistry rpcHandlerRegistry
  ) {
    this.eventManager = eventManager;
    this.networkClient = networkClient;
    this.serviceRegistry = serviceRegistry;
    this.rpcHandlerRegistry = rpcHandlerRegistry;
  }

  /**
   * Copies the caller class location from the class path to the given target path.
   *
   * @param clazz  the class of which the associated class path entry should get copied.
   * @param target the target file to copy the file into - the file is not required to exist.
   * @return true if the entry was copied successfully, false otherwise.
   * @throws NullPointerException if clazz or target is null.
   */
  public boolean copyJarContainingClass(@NonNull Class<?> clazz, @NonNull Path target) {
    try {
      // get the location of the class path entry associated with the given class
      var uri = ResourceResolver.resolveCodeSourceOfClass(clazz);
      // copy the file
      try (var out = Files.newOutputStream(target)) {
        FileUtil.copy(uri.toURL().openStream(), out);
        return true;
      }
    } catch (IOException exception) {
      LOGGER.error("Unable to copy class path entry of {} to {}", clazz, target, exception);
      return false;
    }
  }

  /**
   * Copies the appropriate plugin configuration file for the given service environment type. All the listed files will
   * be copied and renamed to plugin.yml:
   *
   * <ul>
   *   <li>plugin.bungee.yml if the given environment is BUNGEECORD.</li>
   *   <li>plugin.waterdogpe.yml if the given environment is WATERDOG_PE.</li>
   *   <li>plugin.nukkit.yml if the given environment is NUKKIT.</li>
   *   <li>plugin.limboloohp.yml if the given environment is LIMBO_LOOHP.</li>
   *   <li>plugin.bukkit.yml is used as a fallback - if the file does not exist it will not be used.</li>
   * </ul>
   *
   * @param clazz the class of which the associated class path entry should get copied.
   * @param type  the current {@link ServiceEnvironmentType} of the service the file gets copied for.
   * @param file  the target file of the plugin to copy the file for.
   * @throws NullPointerException if clazz, type or file is null.
   */
  public void copyPluginConfigurationFileForEnvironment(
    @NonNull Class<?> clazz,
    @NonNull ServiceEnvironmentType type,
    @NonNull Path file
  ) {
    FileUtil.openZipFile(file, fileSystem -> {
      // check if there is a plugin.yml file already - delete if it exists
      var pluginPath = fileSystem.getPath("plugin.yml");
      if (Files.exists(pluginPath)) {
        Files.delete(pluginPath);
      }
      // select the input stream to copy the file from
      var in = clazz.getClassLoader().getResourceAsStream(String.format(
        "plugin.%s.yml",
        StringUtil.toLower(type.name())));
      // copy the file if the file exists
      if (in != null) {
        Files.copy(in, pluginPath);
        // close the stream to avoid resource leaks
        in.close();
      }
    });
  }

  /**
   * Unregisters all kind of listeners and bindings:
   * <ul>
   *   <li>Registered events in the {@link EventManager}</li>
   *   <li>Registered services in the {@link ServiceRegistry}</li>
   *   <li>Registered bindings for the {@link DefaultObjectMapper#DEFAULT_MAPPER}</li>
   *   <li>Registered rpc handlers in the {@link RPCHandlerRegistry}</li>
   *   <li>Registered packet listeners in the {@link eu.cloudnetservice.driver.network.protocol.PacketListenerRegistry}</li>
   * </ul>
   *
   * @param classLoader the class loader that registered all the mentioned listeners and bindings.
   * @throws NullPointerException if the given class loader is null.
   */
  public void unregisterAll(@NonNull ClassLoader classLoader) {
    // remove all event listeners
    this.eventManager.unregisterListeners(classLoader);
    // remove all registered services
    this.serviceRegistry.unregisterAll(classLoader);
    // remove custom mapper bindings
    DefaultObjectMapper.DEFAULT_MAPPER.unregisterBindings(classLoader);
    // remove rpc handlers
    this.rpcHandlerRegistry.unregisterHandlers(classLoader);
    // remove registered languages
    I18n.i18n().unregisterProviders(classLoader);
    // remove all packet listeners
    this.networkClient.packetRegistry().removeListeners(classLoader);
    for (var channel : this.networkClient.channels()) {
      channel.packetRegistry().removeListeners(classLoader);
    }
  }
}
