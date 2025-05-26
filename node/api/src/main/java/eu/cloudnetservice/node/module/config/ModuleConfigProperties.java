/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.node.module.config;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleConfigKey;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Properties that define how a module configuration container should be created. Configurations can be created based on
 * a constructed properties instance using {@link ModuleConfigRegistry#configContainer(ModuleConfigProperties)}.
 *
 * @param <T> the type of the configuration model that will be wrapped in the final configuration container.
 * @since 4.0
 */
@SuppressWarnings("ClassCanBeRecord") // we want others to be able to extend this
public class ModuleConfigProperties<T> {

  private final ModuleConfigKey key;
  private final Type configModelType;
  private final String storageOverride;
  private final DocumentFactory documentFactory;

  private final List<ModuleConfigFlag> flags;
  private final Supplier<T> defaultValueSupplier;

  /**
   * Constructs a new instance, primarily intended for subclassing. Use {@link #newStandardBuilder()} and use the
   * returned builder to construct an instance instead.
   *
   * @param key                  the key of to use for the config.
   * @param configModelType      the type of the model represented by the config.
   * @param storageOverride      the storage override to use for the config.
   * @param documentFactory      the document factory to use for the config.
   * @param flags                the flags to use for the config.
   * @param defaultValueSupplier the default value supplier for single configurations.
   * @throws NullPointerException if the given key, model type, document factory or flag list is null.
   */
  // impl note: constructor can be called by untrusted code and must be as defensive as possible
  protected ModuleConfigProperties(
    @NonNull ModuleConfigKey key,
    @NonNull Type configModelType,
    @Nullable String storageOverride,
    @NonNull DocumentFactory documentFactory,
    @NonNull List<ModuleConfigFlag> flags,
    @Nullable Supplier<T> defaultValueSupplier
  ) {
    this.key = key;
    this.configModelType = configModelType;
    this.storageOverride = storageOverride;
    this.documentFactory = documentFactory;
    this.flags = List.copyOf(flags);
    this.defaultValueSupplier = defaultValueSupplier;
  }

  /**
   * Constructs a new builder for a module config props instance.
   *
   * @param <T> the type of the configuration model that will be wrapped in the final configuration container.
   * @return a new builder for a module config props instance.
   */
  // impl note: not named builder() on purpose so that the name doesn't shade into extending classes
  @Contract(value = " -> new", pure = true)
  public static @NonNull <T> Builder<T> newStandardBuilder() {
    return new Builder<>();
  }

  /**
   * Constructs a new builder for a module config props instance, copying the values from the given config properties
   * instance.
   *
   * @param properties the base properties instance to copy the values from.
   * @param <T>        the type of the configuration model that will be wrapped in the final configuration container.
   * @return a new builder for a module config props instance.
   * @throws NullPointerException if the given base properties instance is null.
   */
  @Contract("_ -> new")
  public static @NonNull <T> Builder<T> newStandardBuilder(@NonNull ModuleConfigProperties<T> properties) {
    return ModuleConfigProperties.<T>newStandardBuilder()
      .key(properties.key())
      .configModelType(properties.configModelType())
      .storageOverride(properties.storageOverride())
      .documentFactory(properties.documentFactory())

      .flags(properties.flags())
      .defaultValueSupplier(properties.defaultValueSupplier());
  }

  /**
   * Get the key to use by the config container created based on this instance.
   *
   * @return the key to use by the config container created based on this instance.
   */
  public @NonNull ModuleConfigKey key() {
    return this.key;
  }

  /**
   * Get the type that represents the model stored in the configuration.
   *
   * @return the type that represents the model stored in the configuration.
   */
  public @NonNull Type configModelType() {
    return this.configModelType;
  }

  /**
   * Get the name of the module config storage to use instead of the default storage, can be null to indicate that no
   * override is present and the default should be used.
   *
   * @return the name of the module config storage to use instead of the default storage, null to indicate no override.
   */
  public @Nullable String storageOverride() {
    return this.storageOverride;
  }

  /**
   * Get the document factory to use when converting from a stored config to a config document.
   *
   * @return the document factory to use when converting from a stored config to a config document.
   */
  public @NonNull DocumentFactory documentFactory() {
    return this.documentFactory;
  }

  /**
   * Get the flags that should be used for the config container that is created based on this instance. The returned
   * list cannot be modified, use the builder instead.
   *
   * @return the flags that should be used for the config container that is created based on this instance.
   */
  @Unmodifiable
  public @NonNull List<ModuleConfigFlag> flags() {
    return this.flags;
  }

  /**
   * Get the supplier for default values in a module configuration container. Can only be present in combination with
   * non-composite keys, as composite configurations never provide a default config.
   *
   * @return the supplier for default values in a module configuration container.
   */
  public @Nullable Supplier<T> defaultValueSupplier() {
    return this.defaultValueSupplier;
  }

  /**
   * A builder for module configuration properties.
   *
   * @param <T> the type of the configuration model that will be wrapped in the final configuration container.
   * @since 4.0
   */
  public static class Builder<T> {

    private ModuleConfigKey key;
    private Type configModelType;
    private String storageOverride;

    private Supplier<T> defaultValueSupplier;
    private List<ModuleConfigFlag> flags = new ArrayList<>();
    private DocumentFactory documentFactory = DocumentFactory.json();

    /**
     * Constructor for subclassing, use {@link #newStandardBuilder()} for constructing a builder instance.
     */
    protected Builder() {
    }

    /**
     * Sets the key used by the created configuration container. The type of the given key determines the type of
     * configuration container that is being created. A composite key creates a composite container, a non-composite key
     * creates a single config container.
     *
     * @param key the key to use.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given key is null.
     */
    public @NonNull Builder<T> key(@NonNull ModuleConfigKey key) {
      this.key = key;
      return this;
    }

    /**
     * Sets the type that represents the model stored in the configuration. The configuration files are deserialized
     * into the provided config model.
     *
     * @param configModelType the type that represents the model stored in the configuration.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given config model type is null.
     */
    public @NonNull Builder<T> configModelType(@NonNull Class<T> configModelType) {
      this.configModelType = configModelType;
      return this;
    }

    /**
     * Sets the type that represents the model stored in the configuration. The configuration files are deserialized
     * into the provided config model.
     *
     * @param configModelType the type that represents the model stored in the configuration.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given config model type is null.
     */
    public @NonNull Builder<T> configModelType(@NonNull Type configModelType) {
      this.configModelType = configModelType;
      return this;
    }

    /**
     * Sets the name of the storage to use instead of the default storage for the config with the given key. This con be
     * useful when, for example, a config should be migrated from one storage to another. By default, the module system
     * will determine from where a configuration should be loaded.
     *
     * @param storageOverride the name of the storage to the configuration from, null to use the default behavior.
     * @return this builder, for chaining.
     */
    public @NonNull Builder<T> storageOverride(@Nullable String storageOverride) {
      this.storageOverride = storageOverride;
      return this;
    }

    /**
     * Sets the default value supplier to use when the configuration of a single configuration container does not yet
     * exist. A default value cannot be provided for composite configuration containers.
     *
     * @param defaultValueSupplier the default value supplier, null for no default value.
     * @return this builder, for chaining.
     */
    public @NonNull Builder<T> defaultValueSupplier(@Nullable Supplier<T> defaultValueSupplier) {
      this.defaultValueSupplier = defaultValueSupplier;
      return this;
    }

    /**
     * Sets the flags that should be used for the created configuration.
     *
     * @param flags the flags to use for the configuration.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given flags list is null.
     */
    public @NonNull Builder<T> flags(@NonNull List<ModuleConfigFlag> flags) {
      this.flags = new ArrayList<>(flags);
      return this;
    }

    /**
     * Modifies the flags that should be used for the created configuration.
     *
     * @param modifier the modifier to be applied to the config flags of this builder.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given modifier is null.
     */
    public @NonNull Builder<T> modifyFlags(@NonNull Consumer<List<ModuleConfigFlag>> modifier) {
      modifier.accept(this.flags);
      return this;
    }

    /**
     * Sets the document factory to use when converting the config data from a module storage to a document.
     *
     * @param documentFactory the document factory to use for document creation.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given document factory is null.
     */
    public @NonNull Builder<T> documentFactory(@NonNull DocumentFactory documentFactory) {
      this.documentFactory = documentFactory;
      return this;
    }

    /**
     * Asserts that the default value supplier does not result in an invalid combination.
     *
     * @throws IllegalArgumentException if the default value supplier presence results in an invalid combination.
     */
    protected void assertValidDefaultSupplier() {
      if (this.key.compositeKey() && this.defaultValueSupplier != null) {
        throw new IllegalArgumentException("default value supplier is not allowed for composite configs");
      }
    }

    /**
     * Constructs a new module config properties instance based on this builder.
     *
     * @return a new module config properties instance based on this builder.
     * @throws NullPointerException     if no key or document factory was provided to this builder.
     * @throws IllegalArgumentException if a combination of arguments is invalid.
     */
    @Contract(" -> new")
    public @NonNull ModuleConfigProperties<T> build() {
      Objects.requireNonNull(this.key, "config key must be given");
      Objects.requireNonNull(this.configModelType, "config model type must be given");
      Objects.requireNonNull(this.documentFactory, "document factory must be given");
      this.assertValidDefaultSupplier();

      return new ModuleConfigProperties<>(
        this.key,
        this.configModelType,
        this.storageOverride,
        this.documentFactory,
        this.flags,
        this.defaultValueSupplier);
    }
  }
}
