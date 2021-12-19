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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public boolean shouldPreloadClassesBeforeStartup(@NotNull Path applicationFile) {
      try (JarFile file = new JarFile(applicationFile.toFile())) {
        return file.getEntry("META-INF/versions.list") != null;
      } catch (IOException exception) {
        // wtf?
        return false;
      }
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
  ),
  GO_MINT(
    new ServiceEnvironment[]{ServiceEnvironment.GO_MINT_DEFAULT},
    MinecraftServiceType.BEDROCK_SERVER,
    44955
  ) {
    @Override
    public String getMainClass(@Nullable Path applicationFile) {
      return "io.gomint.server.Bootstrap";
    }

    @Override
    public @NotNull String getClasspath(@NotNull Path wrapperFile, @Nullable Path applicationFile) {
      return wrapperFile.toAbsolutePath() + File.pathSeparator + "modules/*";
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
  WATERDOG(
    new ServiceEnvironment[]{ServiceEnvironment.WATERDOG_DEFAULT},
    MinecraftServiceType.BEDROCK_PROXY,
    19132
  ),
  WATERDOG_PE(
    new ServiceEnvironment[]{ServiceEnvironment.WATERDOG_PE},
    MinecraftServiceType.BEDROCK_PROXY,
    19132
  );

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

  @Deprecated
  public @Nullable String getMainClass(@Nullable File applicationFile) {
    return applicationFile == null ? null : this.getMainClass(applicationFile.toPath());
  }

  public @Nullable String getMainClass(@Nullable Path applicationFile) {
    if (applicationFile != null && Files.exists(applicationFile)) {
      try (JarFile jarFile = new JarFile(applicationFile.toFile())) {
        Manifest manifest = jarFile.getManifest();
        return manifest == null ? null : manifest.getMainAttributes().getValue("Main-Class");
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
    return null;
  }

  public boolean shouldPreloadClassesBeforeStartup(@NotNull Path applicationFile) {
    return false;
  }

  @Deprecated
  public @NotNull String getClasspath(@NotNull File wrapperFile, @Nullable File applicationFile) {
    return this.getClasspath(wrapperFile.toPath(), applicationFile == null ? null : applicationFile.toPath());
  }

  public @NotNull String getClasspath(@NotNull Path wrapperFile, @Nullable Path applicationFile) {
    return wrapperFile.toAbsolutePath().toString();
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
