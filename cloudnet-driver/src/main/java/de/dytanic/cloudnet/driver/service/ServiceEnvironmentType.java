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

package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The ServiceEnvironmentType groups the single {@link ServiceEnvironment} and provides methods to retrieve the main
 * class needed for the start of the ServiceEnvironment
 */
public enum ServiceEnvironmentType {

  MINECRAFT_SERVER(
    new ServiceEnvironment[]{
      ServiceEnvironment.MINECRAFT_SERVER_FORGE,
      ServiceEnvironment.MINECRAFT_SERVER_SPONGE_VANILLA,
      ServiceEnvironment.MINECRAFT_SERVER_TACO,
      ServiceEnvironment.MINECRAFT_SERVER_PAPER_SPIGOT,
      ServiceEnvironment.MINECRAFT_SERVER_TUINITY_SPIGOT,
      ServiceEnvironment.MINECRAFT_SERVER_SPIGOT,
      ServiceEnvironment.MINECRAFT_SERVER_AKARIN,
      ServiceEnvironment.MINECRAFT_SERVER_DEFAULT,
    },
    MinecraftServiceType.JAVA_SERVER,
    44955
  ) {
    @Override
    public @NotNull Collection<String> getProcessArguments() {
      return Collections.singleton("nogui");
    }
  },
  GLOWSTONE(new ServiceEnvironment[]{ServiceEnvironment.GLOWSTONE_DEFAULT},
    MinecraftServiceType.JAVA_SERVER,
    44955
  ),
  NUKKIT(
    new ServiceEnvironment[]{ServiceEnvironment.NUKKIT_DEFAULT},
    MinecraftServiceType.BEDROCK_SERVER,
    44955
  ) {
    @Override
    public @NotNull Collection<String> getProcessArguments() {
      return Collections.singleton("disable-ansi");
    }
  },
  BUNGEECORD(
    new ServiceEnvironment[]{
      ServiceEnvironment.BUNGEECORD_HEXACORD,
      ServiceEnvironment.BUNGEECORD_TRAVERTINE,
      ServiceEnvironment.BUNGEECORD_WATERFALL,
      ServiceEnvironment.BUNGEECORD_DEFAULT
    },
    MinecraftServiceType.JAVA_PROXY,
    25565,
    new String[]{">"}
  ),
  VELOCITY(
    new ServiceEnvironment[]{ServiceEnvironment.VELOCITY_DEFAULT},
    MinecraftServiceType.JAVA_PROXY,
    25565
  ),
  WATERDOG_PE(
    new ServiceEnvironment[]{ServiceEnvironment.WATERDOG_PE},
    MinecraftServiceType.BEDROCK_PROXY,
    19132
  );

  public static final ServiceEnvironmentType[] VALUES = ServiceEnvironmentType.values();
  private static final Logger LOGGER = LogManager.getLogger(ServiceEnvironmentType.class);
  private final ServiceEnvironment[] environments;
  private final MinecraftServiceType type;

  private final int defaultStartPort;
  private final Collection<String> ignoredConsoleLines;

  ServiceEnvironmentType(ServiceEnvironment[] environments, MinecraftServiceType type, int defaultStartPort) {
    this(environments, type, defaultStartPort, new String[0]);
  }

  ServiceEnvironmentType(ServiceEnvironment[] environments, MinecraftServiceType type, int defaultStartPort,
    String[] ignoredConsoleLines) {
    this.environments = environments;
    this.type = type;
    this.defaultStartPort = defaultStartPort;
    this.ignoredConsoleLines = Arrays.asList(ignoredConsoleLines);
  }

  public @Nullable String getMainClass(@Nullable Path applicationFile) {
    if (applicationFile != null && Files.exists(applicationFile)) {
      try (JarFile jarFile = new JarFile(applicationFile.toFile())) {
        Manifest manifest = jarFile.getManifest();
        return manifest == null ? null : manifest.getMainAttributes().getValue("Main-Class");
      } catch (IOException exception) {
        LOGGER.severe("Exception while resolving main class", exception);
      }
    }
    return null;
  }

  public @NotNull String getClasspath(@NotNull Path wrapperFile, @Nullable Path applicationFile) {
    return wrapperFile.toAbsolutePath() + File.pathSeparator + (applicationFile == null ? ""
      : applicationFile.toAbsolutePath());
  }

  public @NotNull Collection<String> getProcessArguments() {
    return Collections.emptyList();
  }

  public ServiceEnvironment[] getEnvironments() {
    return this.environments;
  }

  public MinecraftServiceType getMinecraftType() {
    return this.type;
  }

  public boolean isMinecraftJavaProxy() {
    return this.type == MinecraftServiceType.JAVA_PROXY;
  }

  public boolean isMinecraftBedrockProxy() {
    return this.type == MinecraftServiceType.BEDROCK_PROXY;
  }

  public boolean isMinecraftJavaServer() {
    return this.type == MinecraftServiceType.JAVA_SERVER;
  }

  public boolean isMinecraftBedrockServer() {
    return this.type == MinecraftServiceType.BEDROCK_SERVER;
  }

  public boolean isMinecraftProxy() {
    return this.isMinecraftJavaProxy() || this.isMinecraftBedrockProxy();
  }

  public boolean isMinecraftServer() {
    return this.isMinecraftJavaServer() || this.isMinecraftBedrockServer();
  }

  public boolean isMinecraftJava() {
    return this.isMinecraftJavaServer() || this.isMinecraftJavaProxy();
  }

  public boolean isMinecraftBedrock() {
    return this.isMinecraftBedrockServer() || this.isMinecraftBedrockProxy();
  }

  public int getDefaultStartPort() {
    return this.defaultStartPort;
  }

  public Collection<String> getIgnoredConsoleLines() {
    return this.ignoredConsoleLines;
  }
}
