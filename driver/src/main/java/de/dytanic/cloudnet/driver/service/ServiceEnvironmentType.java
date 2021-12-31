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

package de.dytanic.cloudnet.driver.service;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.Nameable;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.property.DocProperty;
import de.dytanic.cloudnet.common.document.property.FunctionalDocProperty;
import de.dytanic.cloudnet.common.document.property.JsonDocPropertyHolder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * The ServiceEnvironmentType groups the single {@link ServiceEnvironment} and provides methods to retrieve the main
 * class needed for the start of the ServiceEnvironment
 */
@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceEnvironmentType extends JsonDocPropertyHolder implements Nameable, Cloneable {

  public static final DocProperty<Boolean> JAVA_PROXY = FunctionalDocProperty.<Boolean>forNamedProperty("isJavaProxy")
    .reader(document -> document.getBoolean("isJavaProxy"))
    .writer((bool, document) -> document.append("isJavaProxy", bool))
    .build();
  public static final DocProperty<Boolean> PE_PROXY = FunctionalDocProperty.<Boolean>forNamedProperty("isPeProxy")
    .reader(document -> document.getBoolean("isPeProxy"))
    .writer((bool, document) -> document.append("isPeProxy", bool))
    .build();
  public static final DocProperty<Boolean> JAVA_SERVER = FunctionalDocProperty.<Boolean>forNamedProperty("isJavaServer")
    .reader(document -> document.getBoolean("isJavaServer"))
    .writer((bool, document) -> document.append("isJavaServer", bool))
    .build();
  public static final DocProperty<Boolean> PE_SERVER = FunctionalDocProperty.<Boolean>forNamedProperty("isPeServer")
    .reader(document -> document.getBoolean("isPeServer"))
    .writer((bool, document) -> document.append("isPeServer", bool))
    .build();

  public static final ServiceEnvironmentType NUKKIT = ServiceEnvironmentType.builder()
    .name("NUKKIT")
    .addDefaultProcessArgument("disable-ansi")
    .properties(JsonDocument.newDocument().property(PE_SERVER, true))
    .build();
  public static final ServiceEnvironmentType MINECRAFT_SERVER = ServiceEnvironmentType.builder()
    .name("MINECRAFT_SERVER")
    .addDefaultProcessArgument("nogui")
    .properties(JsonDocument.newDocument().property(JAVA_SERVER, true))
    .build();
  public static final ServiceEnvironmentType GLOWSTONE = ServiceEnvironmentType.builder()
    .name("GLOWSTONE")
    .properties(JsonDocument.newDocument().property(JAVA_SERVER, true))
    .build();
  public static final ServiceEnvironmentType BUNGEECORD = ServiceEnvironmentType.builder()
    .name("BUNGEECORD")
    .defaultServiceStartPort(25565)
    .properties(JsonDocument.newDocument().property(JAVA_PROXY, true))
    .build();
  public static final ServiceEnvironmentType VELOCITY = ServiceEnvironmentType.builder()
    .name("VELOCITY")
    .defaultServiceStartPort(25565)
    .properties(JsonDocument.newDocument().property(JAVA_PROXY, true))
    .build();
  public static final ServiceEnvironmentType WATERDOG_PE = ServiceEnvironmentType.builder()
    .name("WATERDOG_PE")
    .defaultServiceStartPort(19132)
    .properties(JsonDocument.newDocument().property(PE_PROXY, true))
    .build();

  private final String name;
  private final int defaultServiceStartPort;
  private final Set<String> defaultProcessArguments;

  protected ServiceEnvironmentType(
    @NonNull String name,
    int defaultServiceStartPort,
    @NonNull Set<String> defaultProcessArguments,
    @NonNull JsonDocument properties
  ) {
    this.name = name;
    this.defaultServiceStartPort = defaultServiceStartPort;
    this.defaultProcessArguments = defaultProcessArguments;
    this.properties = properties;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ServiceEnvironmentType type) {
    return builder()
      .name(type.name())
      .properties(type.properties().clone())
      .defaultServiceStartPort(type.defaultStartPort())
      .defaultProcessArguments(type.defaultProcessArguments());
  }

  public static boolean isMinecraftProxy(@NonNull ServiceEnvironmentType type) {
    return JAVA_PROXY.get(type.properties()) || PE_PROXY.get(type.properties());
  }

  public static boolean isMinecraftServer(@NonNull ServiceEnvironmentType type) {
    return JAVA_SERVER.get(type.properties()) || PE_SERVER.get(type.properties());
  }

  @Override
  public @NonNull String name() {
    return this.name;
  }

  public int defaultStartPort() {
    return this.defaultServiceStartPort;
  }

  public @NonNull Collection<String> defaultProcessArguments() {
    return this.defaultProcessArguments;
  }

  @Override
  public ServiceEnvironmentType clone() {
    try {
      return (ServiceEnvironmentType) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  public static class Builder {

    private String name;
    private int defaultServiceStartPort = 44955;
    private JsonDocument properties = JsonDocument.newDocument();
    private Set<String> defaultProcessArguments = new HashSet<>();

    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    public @NonNull Builder defaultServiceStartPort(int defaultServiceStartPort) {
      this.defaultServiceStartPort = defaultServiceStartPort;
      return this;
    }

    public @NonNull Builder properties(@NonNull JsonDocument properties) {
      this.properties = properties;
      return this;
    }

    public @NonNull Builder defaultProcessArguments(@NonNull Collection<String> defaultProcessArguments) {
      this.defaultProcessArguments = new HashSet<>(defaultProcessArguments);
      return this;
    }

    public @NonNull Builder addDefaultProcessArgument(@NonNull String defaultProcessArgument) {
      this.defaultProcessArguments.add(defaultProcessArgument);
      return this;
    }

    public @NonNull ServiceEnvironmentType build() {
      Verify.verifyNotNull(this.name, "no name given");
      Verify.verify(this.defaultServiceStartPort > 0 && this.defaultServiceStartPort <= 65535, "invalid default port");

      return new ServiceEnvironmentType(
        this.name,
        this.defaultServiceStartPort,
        this.defaultProcessArguments,
        this.properties);
    }
  }
}
