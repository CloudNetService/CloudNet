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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import eu.cloudnetservice.driver.base.Named;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.driver.document.property.DocProperty;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * The service environment type groups multiple service environments together and holds all common variables to identify
 * them. Everything which is configured on the service environment type level should apply to all service environments
 * as well.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public final class ServiceEnvironmentType implements DefaultedDocPropertyHolder, Named, Cloneable {

  /**
   * A property set on all service environment types which are a Minecraft java edition proxy.
   */
  public static final DocProperty<Boolean> JAVA_PROXY = DocProperty.property("isJavaProxy", Boolean.class)
    .withDefault(false);
  /**
   * A property set on all service environment types which are a Minecraft pocket edition proxy.
   */
  public static final DocProperty<Boolean> PE_PROXY = DocProperty.property("isPeProxy", Boolean.class)
    .withDefault(false);
  /**
   * A property set on all service environment types which are a Minecraft java edition server.
   */
  public static final DocProperty<Boolean> JAVA_SERVER = DocProperty.property("isJavaServer", Boolean.class)
    .withDefault(false);
  /**
   * A property set on all service environment types which are a Minecraft pocket edition server.
   */
  public static final DocProperty<Boolean> PE_SERVER = DocProperty.property("isPeServer", Boolean.class)
    .withDefault(false);

  /**
   * A property which defines the plugin directory of a service environment. For example for a modded server this would
   * return "mods" rather than "plugins" (which is the default value).
   */
  public static final DocProperty<String> PLUGIN_DIR = DocProperty.property("pluginDir", String.class)
    .withDefault("plugins");

  /**
   * The default nukkit service environment type (Pocket Edition server).
   */
  public static final ServiceEnvironmentType NUKKIT = ServiceEnvironmentType.builder()
    .name("NUKKIT")
    .defaultProcessArguments(Set.of("disable-ansi"))
    .properties(Document.newJsonDocument().writeProperty(PE_SERVER, true))
    .build();
  /**
   * The default minecraft server service environment type. This applies to all services which don't need special
   * configuration to run a wrapped minecraft java edition server in any way (Java Edition server).
   */
  public static final ServiceEnvironmentType MINECRAFT_SERVER = ServiceEnvironmentType.builder()
    .name("MINECRAFT_SERVER")
    .defaultProcessArguments(Set.of("nogui"))
    .properties(Document.newJsonDocument().writeProperty(JAVA_SERVER, true))
    .build();
  /**
   * The default modded server service environment type. This applies to all services which are wrapping a minecraft
   * server instance but allows mods to run on them, for example fabric (Java Edition server).
   */
  public static final ServiceEnvironmentType MODDED_MINECRAFT_SERVER = ServiceEnvironmentType.builder()
    .name("MODDED_MINECRAFT_SERVER")
    .defaultProcessArguments(Set.of("nogui"))
    .properties(Document.newJsonDocument().writeProperty(JAVA_SERVER, true).writeProperty(PLUGIN_DIR, "mods"))
    .build();
  /**
   * The minestom service environment type. This applies to all services which are a server minestom instance allowing
   * extensions running on it.
   */
  public static final ServiceEnvironmentType MINESTOM = ServiceEnvironmentType.builder()
    .name("MINESTOM")
    .properties(Document.newJsonDocument().writeProperty(JAVA_SERVER, true).writeProperty(PLUGIN_DIR, "extensions"))
    .build();
  /**
   * The Limbo (LOOHP) service environment type (Java Edition dummy server).
   */
  public static final ServiceEnvironmentType LIMBO_LOOHP = ServiceEnvironmentType.builder()
    .name("LIMBO_LOOHP")
    .defaultProcessArguments(Set.of("nogui"))
    .properties(Document.newJsonDocument().writeProperty(JAVA_SERVER, true))
    .build();
  /**
   * The bungeecord service environment type, can also be any fork of bungeecord (Java Edition proxy).
   */
  public static final ServiceEnvironmentType BUNGEECORD = ServiceEnvironmentType.builder()
    .name("BUNGEECORD")
    .defaultServiceStartPort(25565)
    .properties(Document.newJsonDocument().writeProperty(JAVA_PROXY, true))
    .build();
  /**
   * The velocity service environment type (Java Edition proxy).
   */
  public static final ServiceEnvironmentType VELOCITY = ServiceEnvironmentType.builder()
    .name("VELOCITY")
    .defaultServiceStartPort(25565)
    .properties(Document.newJsonDocument().writeProperty(JAVA_PROXY, true))
    .build();
  /**
   * The waterdog PE service environment type (PE proxy).
   */
  public static final ServiceEnvironmentType WATERDOG_PE = ServiceEnvironmentType.builder()
    .name("WATERDOG_PE")
    .defaultServiceStartPort(19132)
    .properties(Document.newJsonDocument().writeProperty(PE_PROXY, true))
    .build();

  private final String name;
  private final int defaultServiceStartPort;
  private final Set<String> defaultProcessArguments;

  private final Document properties;

  /**
   * Constructs a new service environment type instance.
   *
   * @param name                    the name of the environment type.
   * @param defaultServiceStartPort the default start port of the environment, used when no specific port is supplied.
   * @param defaultProcessArguments the default process arguments to append to a service startup command line.
   * @param properties              the properties which contain further information about the environment.
   * @throws NullPointerException if one of the given parameters is null.
   */
  private ServiceEnvironmentType(
    @NonNull String name,
    int defaultServiceStartPort,
    @NonNull Set<String> defaultProcessArguments,
    @NonNull Document properties
  ) {
    this.name = name;
    this.defaultServiceStartPort = defaultServiceStartPort;
    this.defaultProcessArguments = defaultProcessArguments;
    this.properties = properties;
  }

  /**
   * Constructs a new builder instance for a service environment type.
   *
   * @return a new service environment builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder instance for a service environment which has the properties of the given service
   * environment already set.
   * <p>
   * When calling build directly after constructing a builder using this method, it will result in an environment type
   * which is equal but not the same as the given one.
   *
   * @param type the environment type to copy the properties of.
   * @return a new environment type builder instance which has the properties of the given environment type set.
   * @throws NullPointerException if the given environment type is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceEnvironmentType type) {
    return builder()
      .name(type.name())
      .properties(type.propertyHolder().immutableCopy())
      .defaultServiceStartPort(type.defaultStartPort())
      .defaultProcessArguments(type.defaultProcessArguments());
  }

  /**
   * Checks if the given environment type is a proxy for Minecraft java or pocket edition.
   *
   * @param type the type to check.
   * @return true if the given type is a proxy for java or pocket edition, false otherwise.
   * @throws NullPointerException if the given service environment type is null.
   */
  public static boolean minecraftProxy(@NonNull ServiceEnvironmentType type) {
    return type.readProperty(JAVA_PROXY) || type.readProperty(PE_PROXY);
  }

  /**
   * Checks if the given environment type is a server for Minecraft java or pocket edition.
   *
   * @param type the type to check.
   * @return true if the given type is a server for java or pocket edition, false otherwise.
   * @throws NullPointerException if the given service environment type is null.
   */
  public static boolean minecraftServer(@NonNull ServiceEnvironmentType type) {
    return type.readProperty(JAVA_SERVER) || type.readProperty(PE_SERVER);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Get the default start port of the environment which will be used when no specific port was used to create a
   * service.
   *
   * @return the default start port of the environment.
   */
  public int defaultStartPort() {
    return this.defaultServiceStartPort;
  }

  /**
   * Get the default arguments which get appended to the end of a service command line.
   *
   * @return the default process arguments for the environment type.
   */
  public @NonNull Collection<String> defaultProcessArguments() {
    return this.defaultProcessArguments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceEnvironmentType clone() {
    try {
      return (ServiceEnvironmentType) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
  }

  /**
   * A builder for a service environment type.
   *
   * @since 4.0
   */
  public static class Builder {

    private String name;
    private int defaultServiceStartPort = 44955;
    private Document properties = Document.emptyDocument();
    private Set<String> defaultProcessArguments = new LinkedHashSet<>();

    /**
     * Sets the name of the service environment type.
     *
     * @param name the name of the environment type.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the default start port to use for the service environment type if no other port was explicitly defined when
     * creating a service which uses the service environment type. If the given port is already in use it will be
     * counted up until a free port was found.
     *
     * @param defaultServiceStartPort the default start port of the service environment type.
     * @return the same instance as used to call the method, for chaining.
     */
    public @NonNull Builder defaultServiceStartPort(int defaultServiceStartPort) {
      this.defaultServiceStartPort = defaultServiceStartPort;
      return this;
    }

    /**
     * Sets the properties of the service environment type. These should at least contain an identifier which type of
     * software is wrapped by the environment type (pe or java edition service).
     *
     * @param properties the properties to use for the environment.
     * @return the same instance as used to call the method for chaining.
     * @throws NullPointerException if the given properties document is null.
     */
    public @NonNull Builder properties(@NonNull Document properties) {
      this.properties = properties.immutableCopy();
      return this;
    }

    /**
     * Sets the default process arguments which should get appended to the command line of all services created using
     * the service environment type.
     * <p>
     * This method will override all previously added process arguments. The collection will be copied into this
     * builder, meaning that changes made to the given collection after the method call will not reflect into the
     * builder and vice-versa.
     *
     * @param defaultProcessArguments the default process arguments to apply to a service command line.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given argument collection is null.
     */
    public @NonNull Builder defaultProcessArguments(@NonNull Collection<String> defaultProcessArguments) {
      this.defaultProcessArguments = new HashSet<>(defaultProcessArguments);
      return this;
    }

    /**
     * Modifies the default process arguments which should get appended to the command line of all services created
     * using the service environment type.
     *
     * @param modifier the modifier to be applied to the already added default process arguments of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given argument collection is null.
     */
    public @NonNull Builder modifyDefaultProcessArguments(@NonNull Consumer<Collection<String>> modifier) {
      modifier.accept(this.defaultProcessArguments);
      return this;
    }

    /**
     * Builds a service environment type instance based on the properties supplied to this builder.
     *
     * @return a new service environment type.
     * @throws NullPointerException     if no name was given.
     * @throws IllegalArgumentException if the given default port is out of range.
     */
    public @NonNull ServiceEnvironmentType build() {
      Preconditions.checkNotNull(this.name, "no name given");
      Preconditions.checkArgument(this.defaultServiceStartPort >= 0 && this.defaultServiceStartPort <= 0xFFFF,
        "invalid default port");

      return new ServiceEnvironmentType(
        this.name,
        this.defaultServiceStartPort,
        ImmutableSet.copyOf(this.defaultProcessArguments),
        this.properties);
    }
  }
}
