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

package eu.cloudnetservice.driver.module.driver;

import com.google.gson.JsonSyntaxException;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.DefaultModule;
import eu.cloudnetservice.driver.module.Module;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * Represents a cloudnet driver specific implementation for the module. Usually this should be used as entry point of a
 * module.
 *
 * @see ModuleTask
 * @see Module
 * @see DefaultModule
 * @since 4.0
 */
public class DriverModule extends DefaultModule {

  /**
   * Reads the configuration file of this module from the default or overridden configuration path (via module.json)
   * into a json document, throwing an exception when the document is invalid.
   *
   * @return the config at the default path.
   * @throws JsonSyntaxException if the document is invalid.
   * @see JsonDocument#newDocument(Path)
   */
  public @NonNull JsonDocument readConfig() {
    return JsonDocument.newDocument(this.configPath());
  }

  /**
   * Writes the given {@link JsonDocument} to the default configuration path {@link DriverModule#configPath()}.
   *
   * @param config the config to write.
   * @see JsonDocument#write(Path)
   */
  public void writeConfig(@NonNull JsonDocument config) {
    config.write(this.configPath());
  }

  /**
   * Reads the config of this module to the given model type, creating a new configuration if necessary. This method
   * rethrows the parsing exception wrapped if the reader was unable to read the config model (for example an exception
   * happens during the model instantiation with the supplied arguments by the user in the configuration).
   * <p>
   * The config factory is allowed to return null, an empty json object will be written to the file in that case.
   *
   * @param configModelType      the modeling class of the configuration.
   * @param defaultConfigFactory a factory constructing a default config instance if needed.
   * @param <T>                  the type of the configuration model.
   * @return a newly created default config instance or the read config instance from the config path.
   * @throws NullPointerException                if either the given config model or config factory is null.
   * @throws ModuleConfigurationInvalidException if the reader is unable to read the configuration model from the file.
   */
  public @NonNull <T> T readConfig(@NonNull Class<T> configModelType, @NonNull Supplier<T> defaultConfigFactory) {
    // check if the config already exists, create a default one if not
    if (Files.notExists(this.configPath())) {
      var config = defaultConfigFactory.get();
      this.writeConfig(JsonDocument.newDocument(config));
      return config;
    } else {
      // either we can read the config to the given model type, or we return null and let the module handle it
      try {
        return this.readConfig().toInstanceOf(configModelType);
      } catch (Exception exception) {
        // wrap and rethrow the exception
        throw new ModuleConfigurationInvalidException(this.configPath(), exception);
      }
    }
  }

  /**
   * The default configuration path located in the directory for this module. By default, this is
   * "Module-Name/config.json".
   *
   * @return the path of the config.
   * @see ModuleWrapper#dataDirectory()
   */
  protected @NonNull Path configPath() {
    return this.moduleWrapper().dataDirectory().resolve("config.json");
  }

  /**
   * Registers the given listener to the {@link EventManager}. Down calls to {@link CloudNetDriver#eventManager()} and
   * {@link EventManager#registerListeners(Object...)}.
   *
   * @param listener the listeners to register
   * @return the EventManager that was used to register the listeners.
   */
  @Deprecated(forRemoval = true) // TODO: need to update modules first
  public final @NonNull EventManager registerListener(Object @NonNull ... listener) {
    return this.eventManager().registerListeners(listener);
  }

  /**
   * Gets the {@link ServiceRegistry} of the driver.
   *
   * @return the ServiceRegistry.
   * @see CloudNetDriver#serviceRegistry()
   */
  @Deprecated(forRemoval = true) // TODO: need to update modules first
  public final @NonNull ServiceRegistry serviceRegistry() {
    return this.driver().serviceRegistry();
  }

  /**
   * Gets the {@link EventManager} of the driver.
   *
   * @return the EventManager.
   * @see CloudNetDriver#eventManager()
   */
  @Deprecated(forRemoval = true) // TODO: need to update modules first
  public final @NonNull EventManager eventManager() {
    return this.driver().eventManager();
  }

  /**
   * Gets the {@link RPCFactory} of the driver.
   *
   * @return the RPCProviderFactory.
   * @see CloudNetDriver#rpcFactory()
   */
  @Deprecated(forRemoval = true) // TODO: need to update modules first
  public final @NonNull RPCFactory rpcFactory() {
    return this.driver().rpcFactory();
  }

  /**
   * Gets the {@link CloudNetDriver} instance.
   *
   * @return the CloudNetDriver instance.
   * @see CloudNetDriver#instance()
   */
  @Contract(pure = true)
  @Deprecated(forRemoval = true) // TODO: need to update modules first
  public final @NonNull CloudNetDriver driver() {
    return CloudNetDriver.instance();
  }
}
