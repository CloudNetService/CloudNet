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

package de.dytanic.cloudnet.service.defaults;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.handler.CloudServiceHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public abstract class DefaultMinecraftCloudService extends DefaultTemplateCloudService {

  public DefaultMinecraftCloudService(String runtime, ICloudServiceManager cloudServiceManager,
    ServiceConfiguration serviceConfiguration, @NotNull CloudServiceHandler handler) {
    super(runtime, cloudServiceManager, serviceConfiguration, handler);
  }

  @Override
  protected void preStart() {
    super.preStart();
    try {
      this.configureServiceEnvironment();
    } catch (IOException exception) {
      throw new Error(exception);
    }
  }

  private void rewriteBungeeConfig(Path config) throws IOException {
    this.rewriteServiceConfigurationFile(config, line -> {
      if (line.startsWith("    host: ")) {
        line = "    host: " + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration()
          .getPort();
      } else if (line.startsWith("  host: ")) {
        line = "  host: " + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration()
          .getPort();
      }
      return line;
    });
  }

  private void rewriteVelocityConfig(Path config) throws IOException {
    AtomicBoolean reference = new AtomicBoolean(true);
    this.rewriteServiceConfigurationFile(config, line -> {
      if (line.startsWith("bind =") && reference.getAndSet(false)) {
        return "bind = \"" + CloudNet.getInstance().getConfig().getHostAddress() + ":" + this.getServiceConfiguration()
          .getPort() + "\"";
      }
      return line;
    });
  }

  private void configureServiceEnvironment() throws IOException {
    ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider()
      .getServiceTask(super.serviceConfiguration.getServiceId().getTaskName());
    boolean rewriteIp = serviceTask == null || !serviceTask.isDisableIpRewrite();

    switch (this.getServiceConfiguration().getProcessConfig().getEnvironment()) {
      case BUNGEECORD: {
        if (rewriteIp) {
          Path configLocation = this.getDirectoryPath().resolve("config.yml");
          this.copyDefaultFile("files/bungee/config.yml", configLocation);
          this.rewriteBungeeConfig(configLocation);
        }
        break;
      }
      case WATERDOG: {
        if (rewriteIp) {
          Path configLocation = this.getDirectoryPath().resolve("config.yml");
          this.copyDefaultFile("files/waterdog/config.yml", configLocation);
          this.rewriteBungeeConfig(configLocation);
        }
        break;
      }
      case WATERDOG_PE: {
        if (rewriteIp) {
          Path configLocation = this.getDirectoryPath().resolve("config.yml");
          this.copyDefaultFile("files/waterdogpe/config.yml", configLocation);
          this.rewriteBungeeConfig(configLocation);
        }
        break;
      }
      case VELOCITY: {
        if (rewriteIp) {
          Path configLocation = this.getDirectoryPath().resolve("velocity.toml");
          this.copyDefaultFile("files/velocity/velocity.toml", configLocation);
          this.rewriteVelocityConfig(configLocation);
        }
        break;
      }
      case MINECRAFT_SERVER: {
        Path path = this.getDirectoryPath().resolve("server.properties");
        this.copyDefaultFile("files/nms/server.properties", path);

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
          properties.load(inputStream);
        }

        properties.setProperty("server-name", this.getServiceId().getName());
        if (rewriteIp) {
          properties.setProperty("server-port", String.valueOf(this.getServiceConfiguration().getPort()));
          properties.setProperty("server-ip", CloudNet.getInstance().getConfig().getHostAddress());
        }

        try (OutputStream outputStream = Files.newOutputStream(path)) {
          properties.store(outputStream, "Edit by CloudNet");
        }

        properties.clear();
        // eula auto agree
        properties.setProperty("eula", "true");
        try (OutputStream outputStream = Files.newOutputStream(this.getDirectoryPath().resolve("eula.txt"))) {
          properties.store(outputStream, "Auto Eula agreement by CloudNet");
        }
        break;
      }
      case NUKKIT: {
        if (rewriteIp) {
          Path path = this.getDirectoryPath().resolve("server.properties");
          this.copyDefaultFile("files/nukkit/server.properties", path);

          Properties properties = new Properties();
          try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
          }

          properties.setProperty("server-port", String.valueOf(this.getServiceConfiguration().getPort()));
          properties.setProperty("server-ip", CloudNet.getInstance().getConfig().getHostAddress());

          try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, "Edit by CloudNet");
          }
        }
        break;
      }
      case GO_MINT: {
        if (rewriteIp) {
          Path path = this.getDirectoryPath().resolve("server.yml");
          this.copyDefaultFile("files/gomint/server.yml", path);

          this.rewriteServiceConfigurationFile(path, line -> {
            if (line.startsWith("  ip: ")) {
              line = "  ip: " + CloudNet.getInstance().getConfig().getHostAddress();
            }

            if (line.startsWith("  port: ")) {
              line = "  port: " + this.serviceConfiguration.getPort();
            }

            return line;
          });
        }
        break;
      }
      case GLOWSTONE: {
        if (rewriteIp) {
          Path path = this.getDirectoryPath().resolve("config/glowstone.yml");
          FileUtils.createDirectoryReported(path.getParent());
          this.copyDefaultFile("files/glowstone/glowstone.yml", path);

          this.rewriteServiceConfigurationFile(path, line -> {
            if (line.startsWith("    ip: ")) {
              line = "    ip: '" + CloudNet.getInstance().getConfig().getHostAddress() + "'";
            }

            if (line.startsWith("    port: ")) {
              line = "    port: " + this.getServiceConfiguration().getPort();
            }

            return line;
          });
        }
        break;
      }
      default:
        break;
    }
  }

  private void copyDefaultFile(String from, Path target) throws IOException {
    if (Files.notExists(target)) {
      try (InputStream stream = JVMCloudService.class.getClassLoader().getResourceAsStream(from)) {
        if (stream != null) {
          try (OutputStream targetLocation = Files.newOutputStream(target)) {
            FileUtils.copy(stream, targetLocation);
          }
        }
      }
    }
  }

  private void rewriteServiceConfigurationFile(Path file, UnaryOperator<String> unaryOperator) throws IOException {
    List<String> lines = Files.readAllLines(file)
      .stream()
      .map(unaryOperator)
      .collect(Collectors.toList());
    try (OutputStream outputStream = Files.newOutputStream(file)) {
      for (String replacedLine : lines) {
        outputStream.write((replacedLine + '\n').getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      }
    }
  }

  protected void postConfigureServiceEnvironmentStartParameters(List<String> commandArguments) {
    switch (this.getServiceConfiguration().getProcessConfig().getEnvironment()) {
      case MINECRAFT_SERVER:
        commandArguments.add("nogui");
        break;
      case NUKKIT:
        commandArguments.add("disable-ansi");
        break;
      default:
        break;
    }
  }
}
