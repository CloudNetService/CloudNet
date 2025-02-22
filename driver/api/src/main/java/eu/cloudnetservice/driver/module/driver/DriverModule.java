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

package eu.cloudnetservice.driver.module.driver;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.util.Qualifiers;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.DocumentParseException;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.DefaultModule;
import eu.cloudnetservice.driver.module.Module;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;

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

  protected static final Element CONFIG_PATH_ELEMENT = Element.forType(Path.class)
    .requireAnnotation(Qualifiers.named("configPath"));

  /**
   * Reads the configuration file of this module from the default or overridden configuration path (via module.json)
   * into a json document, throwing an exception when the document is invalid.
   *
   * @param factory the config factory to use when parsing the configuration.
   * @return the config at the default path.
   * @throws DocumentParseException if the document cannot be parsed from the given path.
   */
  public @NonNull Document readConfig(@NonNull DocumentFactory factory) {
    return factory.parse(this.configPath());
  }

  /**
   * Writes the given configuration document to the default configuration path {@link DriverModule#configPath()}.
   *
   * @param config the config to write.
   */
  public void writeConfig(@NonNull Document config) {
    config.writeTo(this.configPath());
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
   * @param documentFactory      the document factory to use when reading the configuration file.
   * @param <T>                  the type of the configuration model.
   * @return a newly created default config instance or the read config instance from the config path.
   * @throws NullPointerException                if either the given config model or config factory is null.
   * @throws ModuleConfigurationInvalidException if the reader is unable to read the configuration model from the file.
   */
  public @NonNull <T> T readConfig(
    @NonNull Class<T> configModelType,
    @NonNull Supplier<T> defaultConfigFactory,
    @NonNull DocumentFactory documentFactory
  ) {
    // check if the config already exists, create a default one if not
    if (Files.notExists(this.configPath())) {
      var config = defaultConfigFactory.get();
      this.writeConfig(documentFactory.newDocument().appendTree(config));
      return config;
    } else {
      // either we can read the config to the given model type, or we return null and let the module handle it
      try {
        return documentFactory.parse(this.configPath()).toInstanceOf(configModelType);
      } catch (Exception exception) {
        // wrap and rethrow the exception
        throw new ModuleConfigurationInvalidException(this.configPath(), exception);
      }
    }
  }

  /**
   * Reads the module configuration file and converts the type of the configuration to the given model type, creating
   * the configuration if necessary. Any exceptions thrown when reading the configuration are forwarded to the caller.
   * <p>
   * Then the given class type is instantiated with the loaded config and config path being known while creating the
   * class instance. Bindings are respected by this method.
   * <p>
   * If further customization of the injection context which is taking over the injection process is needed, the method
   * {@link #readConfigAndInstantiate(InjectionLayer, Class, Supplier, Class, Consumer, DocumentFactory)} is able to
   * allow for further customization of the class injection context (for example needed if other non-constructable types
   * are required to instantiate the binding of the given type).
   *
   * @param injectionLayer       the injection layer to use when instantiating the given type to instantiate.
   * @param configModelType      the modeling class of the configuration.
   * @param defaultConfigFactory a factory constructing a default config instance if needed.
   * @param classToInstantiate   the class to instantiate after successfully loading the configuration.
   * @param documentFactory      the document factory to use when reading the configuration file.
   * @param <C>                  the type of the configuration model.
   * @param <T>                  the type modeling the class which should be instantiated.
   * @return the constructed instance of the given type, constructed with the configuration known to the context.
   * @throws NullPointerException                  if one of the given parameters is null.
   * @throws ModuleConfigurationInvalidException   if the reader is unable to read the configuration from the file.
   * @throws dev.derklaro.aerogel.AerogelException if no binding for T is present and no JIT binding can be created.
   */
  public @NonNull <C, T> T readConfigAndInstantiate(
    @NonNull InjectionLayer<?> injectionLayer,
    @NonNull Class<C> configModelType,
    @NonNull Supplier<C> defaultConfigFactory,
    @NonNull Class<T> classToInstantiate,
    @NonNull DocumentFactory documentFactory
  ) {
    return this.readConfigAndInstantiate(
      injectionLayer,
      configModelType,
      defaultConfigFactory,
      classToInstantiate,
      $ -> {
      },
      documentFactory);
  }

  /**
   * Reads the module configuration file and converts the type of the configuration to the given model type, creating
   * the configuration if necessary. Any exceptions thrown when reading the configuration are forwarded to the caller.
   * <p>
   * Afterwards the given class type is instantiated with the loaded config and config path being known while creating
   * the class instance. Bindings are respected by this method.
   * <p>
   * If further customization of the injection context which is taking over the injection process is needed, the builder
   * consumer passed to this method will be called <strong>after</strong> the overrides for the config type and config
   * path were installed to it. This means that the decorator is able to override the previously installed overrides in
   * the given builder.
   *
   * @param injectionLayer       the injection layer to use when instantiating the given type to instantiate.
   * @param configModelType      the modeling class of the configuration.
   * @param defaultConfigFactory a factory constructing a default config instance if needed.
   * @param classToInstantiate   the class to instantiate after successfully loading the configuration.
   * @param builderDecorator     the decorator to apply to the injection context builder, for further customization.
   * @param documentFactory      the document factory to use when reading the configuration file.
   * @param <C>                  the type of the configuration model.
   * @param <T>                  the type modeling the class which should be instantiated.
   * @return the constructed instance of the given type, constructed with the configuration known to the context.
   * @throws NullPointerException                  if one of the given parameters is null.
   * @throws ModuleConfigurationInvalidException   if the reader is unable to read the configuration from the file.
   * @throws dev.derklaro.aerogel.AerogelException if no binding for T is present and no JIT binding can be created.
   */
  public @NonNull <C, T> T readConfigAndInstantiate(
    @NonNull InjectionLayer<?> injectionLayer,
    @NonNull Class<C> configModelType,
    @NonNull Supplier<C> defaultConfigFactory,
    @NonNull Class<T> classToInstantiate,
    @NonNull Consumer<InjectionContext.Builder> builderDecorator,
    @NonNull DocumentFactory documentFactory
  ) {
    // read the config
    var config = this.readConfig(configModelType, defaultConfigFactory, documentFactory);
    return injectionLayer.instance(classToInstantiate, builder -> {
      // write the default elements to the builder
      builder.override(configModelType, config);
      builder.override(CONFIG_PATH_ELEMENT, this.configPath());
      // apply the custom modifier
      builderDecorator.accept(builder);
    });
  }

  /**
   * The default configuration path located in the directory for this module. By default, this is
   * "Module-Name/config.json".
   *
   * @return the path of the config.
   * @see ModuleWrapper#dataDirectory()
   */
  public @NonNull Path configPath() {
    return this.moduleWrapper().dataDirectory().resolve("config.json");
  }
}
