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

package de.dytanic.cloudnet.config;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.util.NetworkAddressUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public final class JsonConfiguration implements IConfiguration {

  private static final Path CONFIG_FILE_PATH = Paths.get(
    System.getProperty("cloudnet.config.json.path", "config.json"));

  private static final Function<String, SSLConfiguration> SSL_CONFIG_PARSER = value -> {
    String[] values = value.split(";");
    if (values.length == 4 || values.length == 5) {
      return new SSLConfiguration(
        Boolean.parseBoolean(values[0]),
        Boolean.parseBoolean(values[1]),
        values.length == 5 ? Paths.get(values[2]) : null,
        Paths.get(values[values.length - 2]),
        Paths.get(values[values.length - 1]));
    }
    // unable to parse
    return null;
  };

  private NetworkClusterNode identity;
  private NetworkCluster clusterConfig;

  private Collection<String> ipWhitelist;

  private double maxCPUUsageToStartServices;

  private int maxMemory;
  private int maxServiceConsoleLogCacheSize;
  private int processTerminationTimeoutSeconds;

  private Boolean printErrorStreamLinesFromServices;
  private Boolean runBlockedServiceStartTryLaterAutomatic;

  private DefaultJVMFlags defaultJVMFlags;

  private String jvmCommand;
  private String hostAddress;
  private String connectHostAddress;

  private Collection<HostAndPort> httpListeners;

  private SSLConfiguration clientSslConfig;
  private SSLConfiguration serverSslConfig;
  private SSLConfiguration webSslConfig;

  private JsonDocument properties;

  public JsonConfiguration() {
    // TODO: remove in 4.1
    Path oldRegistry = Paths.get("local", "registry");
    if (Files.exists(oldRegistry)) {
      JsonDocument entries = JsonDocument.newDocument(oldRegistry).getDocument("entries");
      if (entries != null) {
        this.properties = JsonDocument.newDocument();
        this.properties.append(entries);
      }
      // remove the old file
      FileUtils.delete(oldRegistry);
    }
  }

  public static @NotNull IConfiguration loadFromFile() {
    if (Files.notExists(CONFIG_FILE_PATH)) {
      JsonConfiguration configuration = new JsonConfiguration();
      configuration.load(); // initializes all fields with the default values
      return configuration.save();
    } else {
      return JsonDocument.newDocument(CONFIG_FILE_PATH).toInstanceOf(JsonConfiguration.class).load();
    }
  }

  @Override
  public boolean isFileExists() {
    return Files.exists(CONFIG_FILE_PATH);
  }

  @Override
  public @NotNull IConfiguration load() {
    if (identity == null) {
      this.identity = new NetworkClusterNode(
        ConfigurationUtils.get(
          "cloudnet.config.identity",
          "Node-" + StringUtil.generateRandomString(4)),
        ConfigurationUtils.get(
          "cloudnet.config.listeners",
          new HostAndPort[]{new HostAndPort(NetworkAddressUtil.getLocalAddress(), 1410)},
          ConfigurationUtils.HOST_AND_PORT_PARSER));
    }

    if (this.clusterConfig == null) {
      this.clusterConfig = new NetworkCluster(
        ConfigurationUtils.get("cloudnet.cluster.id", UUID.randomUUID(), UUID::fromString),
        ConfigurationUtils.get(
          "cloudnet.config.clusterConfig",
          Collections.emptyList(),
          value -> {
            Collection<NetworkClusterNode> nodes = new HashSet<>();
            // collection entries are seperated by ','
            String[] entries = value.split(",");
            for (String entry : entries) {
              // split at '-': <name>-<listeners>
              String[] info = entry.split("-");
              if (info.length == 2) {
                nodes.add(new NetworkClusterNode(info[0], ConfigurationUtils.HOST_AND_PORT_PARSER.apply(info[1])));
              }
            }
            return nodes;
          }));
    }

    if (this.ipWhitelist == null) {
      this.ipWhitelist = ConfigurationUtils.get(
        "cloudnet.config.ipWhitelist",
        NetworkAddressUtil.getAvailableIpAddresses(),
        value -> Arrays.asList(value.split(",")));
    }

    if (this.maxCPUUsageToStartServices <= 0) {
      this.maxCPUUsageToStartServices = ConfigurationUtils.get(
        "cloudnet.config.maxCPUUsageToStartServices",
        90D,
        Double::parseDouble);
    }

    if (this.maxMemory <= 0) {
      this.maxMemory = ConfigurationUtils.get(
        "cloudnet.config.maxMemory",
        (int) ((ProcessSnapshot.OS_BEAN.getTotalPhysicalMemorySize() / (1024 * 1024)) - 512),
        Integer::parseInt);
    }

    if (this.maxServiceConsoleLogCacheSize <= 0) {
      this.maxServiceConsoleLogCacheSize = ConfigurationUtils.get(
        "cloudnet.config.maxServiceConsoleLogCacheSize",
        64,
        Integer::parseInt);
    }

    if (this.processTerminationTimeoutSeconds <= 0) {
      this.processTerminationTimeoutSeconds = ConfigurationUtils.get(
        "cloudnet.config.processTerminationTimeoutSeconds",
        5,
        Integer::parseInt);
    }

    if (this.printErrorStreamLinesFromServices == null) {
      this.printErrorStreamLinesFromServices = ConfigurationUtils.get(
        "cloudnet.config.printErrorStreamLinesFromServices",
        true,
        Boolean::parseBoolean);
    }

    if (this.runBlockedServiceStartTryLaterAutomatic == null) {
      this.runBlockedServiceStartTryLaterAutomatic = ConfigurationUtils.get(
        "cloudnet.config.runBlockedServiceStartTryLaterAutomatic",
        true,
        Boolean::parseBoolean);
    }

    if (this.defaultJVMFlags == null) {
      this.defaultJVMFlags = ConfigurationUtils.get(
        "cloudnet.config.defaultJVMFlags",
        DefaultJVMFlags.DYTANIC,
        value -> Enums.getIfPresent(DefaultJVMFlags.class, value.toUpperCase()).orNull());
    }

    if (this.jvmCommand == null) {
      this.jvmCommand = ConfigurationUtils.get("cloudnet.config.jvmCommand", "java");
    }

    if (this.hostAddress == null) {
      this.hostAddress = ConfigurationUtils.get("cloudnet.config.hostAddress", NetworkAddressUtil.getLocalAddress());
    }

    if (this.connectHostAddress == null) {
      this.connectHostAddress = ConfigurationUtils.get("cloudnet.config.connectHostAddress", this.hostAddress);
    }

    if (this.httpListeners == null) {
      this.httpListeners = ConfigurationUtils.get(
        "cloudnet.config.httpListeners",
        new ArrayList<>(Collections.singleton(new HostAndPort("0.0.0.0", 2812))),
        value -> new ArrayList<>(Arrays.asList(ConfigurationUtils.HOST_AND_PORT_PARSER.apply(value))));
    }

    if (this.clientSslConfig == null) {
      this.clientSslConfig = ConfigurationUtils.get(
        "cloudnet.config.clientSslConfig",
        new SSLConfiguration(
          false,
          false,
          null,
          Paths.get("cert.pem"),
          Paths.get("private.pem")),
        SSL_CONFIG_PARSER);
    }

    if (this.serverSslConfig == null) {
      this.serverSslConfig = ConfigurationUtils.get(
        "cloudnet.config.serverSslConfig",
        new SSLConfiguration(
          false,
          false,
          null,
          Paths.get("cert.pem"),
          Paths.get("private.pem")),
        SSL_CONFIG_PARSER);
    }

    if (this.webSslConfig == null) {
      this.webSslConfig = ConfigurationUtils.get(
        "cloudnet.config.webSslConfig",
        new SSLConfiguration(
          false,
          false,
          null,
          Paths.get("cert.pem"),
          Paths.get("private.pem")),
        SSL_CONFIG_PARSER);
    }

    if (this.properties == null) {
      this.properties = ConfigurationUtils.get(
        "cloudnet.config.properties",
        JsonDocument.newDocument("database_provider", "xodus"),
        JsonDocument::newDocument);
    }

    return this.save();
  }

  @Override
  public @NotNull IConfiguration save() {
    JsonDocument.newDocument(this).write(CONFIG_FILE_PATH);
    return this;
  }

  @Override
  public @NotNull NetworkClusterNode getIdentity() {
    return this.identity;
  }

  @Override
  public void setIdentity(@NotNull NetworkClusterNode identity) {
    this.identity = identity;
  }

  @Override
  public @NotNull NetworkCluster getClusterConfig() {
    return this.clusterConfig;
  }

  @Override
  public void setClusterConfig(@NotNull NetworkCluster clusterConfig) {
    Preconditions.checkNotNull(clusterConfig);

    this.clusterConfig = clusterConfig;
  }

  @Override
  public @NotNull Collection<String> getIpWhitelist() {
    return this.ipWhitelist != null ? this.ipWhitelist : (this.ipWhitelist = new ArrayList<>());
  }

  @Override
  public void setIpWhitelist(@NotNull Collection<String> whitelist) {
    Preconditions.checkNotNull(whitelist);

    this.ipWhitelist = whitelist;
  }

  @Override
  public double getMaxCPUUsageToStartServices() {
    return this.maxCPUUsageToStartServices;
  }

  @Override
  public void setMaxCPUUsageToStartServices(double value) {
    this.maxCPUUsageToStartServices = value;
  }

  @Override
  public boolean isRunBlockedServiceStartTryLaterAutomatic() {
    return this.runBlockedServiceStartTryLaterAutomatic;
  }

  @Override
  public void setRunBlockedServiceStartTryLaterAutomatic(boolean runBlockedServiceStartTryLaterAutomatic) {
    this.runBlockedServiceStartTryLaterAutomatic = runBlockedServiceStartTryLaterAutomatic;
  }

  @Override
  public int getMaxMemory() {
    return this.maxMemory;
  }

  @Override
  public void setMaxMemory(int memory) {
    this.maxMemory = memory;
  }

  @Override
  public int getMaxServiceConsoleLogCacheSize() {
    return this.maxServiceConsoleLogCacheSize;
  }

  @Override
  public void setMaxServiceConsoleLogCacheSize(int maxServiceConsoleLogCacheSize) {
    this.maxServiceConsoleLogCacheSize = maxServiceConsoleLogCacheSize;
  }

  @Override
  public boolean isPrintErrorStreamLinesFromServices() {
    return this.printErrorStreamLinesFromServices;
  }

  @Override
  public void setPrintErrorStreamLinesFromServices(boolean printErrorStreamLinesFromServices) {
    this.printErrorStreamLinesFromServices = printErrorStreamLinesFromServices;
  }

  @Override
  public @NotNull DefaultJVMFlags getDefaultJVMFlags() {
    return this.defaultJVMFlags;
  }

  @Override
  public void setDefaultJVMFlags(@NotNull DefaultJVMFlags defaultJVMFlags) {
    this.defaultJVMFlags = defaultJVMFlags;
  }

  @Override
  public @NotNull String getHostAddress() {
    return this.hostAddress;
  }

  @Override
  public void setHostAddress(@NotNull String hostAddress) {
    this.hostAddress = hostAddress;
  }

  @Override
  public @NotNull Collection<HostAndPort> getHttpListeners() {
    return this.httpListeners != null ? this.httpListeners : (this.httpListeners = new ArrayList<>());
  }

  @Override
  public void setHttpListeners(@NotNull Collection<HostAndPort> httpListeners) {
    Preconditions.checkNotNull(httpListeners);

    this.httpListeners = httpListeners;
  }

  @Override
  public @NotNull String getConnectHostAddress() {
    return this.connectHostAddress;
  }

  @Override
  public void setConnectHostAddress(@NotNull String connectHostAddress) {
    this.connectHostAddress = connectHostAddress;
  }

  @Override
  public @NotNull SSLConfiguration getClientSslConfig() {
    return this.clientSslConfig;
  }

  @Override
  public void setClientSslConfig(@NotNull SSLConfiguration clientSslConfig) {
    this.clientSslConfig = clientSslConfig;
  }

  @Override
  public @NotNull SSLConfiguration getServerSslConfig() {
    return this.serverSslConfig;
  }

  @Override
  public void setServerSslConfig(@NotNull SSLConfiguration serverSslConfig) {
    this.serverSslConfig = serverSslConfig;
  }

  @Override
  public @NotNull SSLConfiguration getWebSslConfig() {
    return this.webSslConfig;
  }

  @Override
  public void setWebSslConfig(@NotNull SSLConfiguration webSslConfig) {
    this.webSslConfig = webSslConfig;
  }

  @Override
  public @NotNull String getJVMCommand() {
    return this.jvmCommand;
  }

  @Override
  public void setJVMCommand(@NotNull String jvmCommand) {
    this.jvmCommand = jvmCommand;
  }

  @Override
  public int getProcessTerminationTimeoutSeconds() {
    return Math.max(1, this.processTerminationTimeoutSeconds);
  }

  @Override
  public void setProcessTerminationTimeoutSeconds(int processTerminationTimeoutSeconds) {
    this.processTerminationTimeoutSeconds = processTerminationTimeoutSeconds;
  }

  @Override
  public @NotNull JsonDocument getProperties() {
    return this.properties;
  }

  @Override
  public void setProperties(@NotNull JsonDocument properties) {
    this.properties = properties;
  }
}
