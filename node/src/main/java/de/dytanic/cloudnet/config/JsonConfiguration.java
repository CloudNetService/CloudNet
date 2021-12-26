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
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.setup.DefaultConfigSetup;
import de.dytanic.cloudnet.util.NetworkAddressUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.NonNull;

public final class JsonConfiguration implements Configuration {

  public static final Path CONFIG_FILE_PATH = Path.of(
    System.getProperty("cloudnet.config.json.path", "config.json"));

  private static final Function<String, SSLConfiguration> SSL_CONFIG_PARSER = value -> {
    var values = value.split(";");
    if (values.length == 4 || values.length == 5) {
      return new SSLConfiguration(
        Boolean.parseBoolean(values[0]),
        Boolean.parseBoolean(values[1]),
        values.length == 5 ? Path.of(values[2]) : null,
        Path.of(values[values.length - 2]),
        Path.of(values[values.length - 1]));
    }
    // unable to parse
    return null;
  };

  private NetworkClusterNode identity;
  private NetworkCluster clusterConfig;

  private Set<String> ipWhitelist;

  private double maxCPUUsageToStartServices;

  private int maxMemory;
  private int maxServiceConsoleLogCacheSize;
  private int processTerminationTimeoutSeconds;

  private Boolean forceInitialClusterDataSync;
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
    var oldRegistry = Path.of("local", "registry");
    if (Files.exists(oldRegistry)) {
      var entries = JsonDocument.newDocument(oldRegistry).getDocument("entries");
      if (!entries.empty()) {
        this.properties = JsonDocument.newDocument();
        this.properties.append(entries);
      }
      // remove the old file
      FileUtils.delete(oldRegistry);
    }
  }

  public static @NonNull Configuration loadFromFile(@NonNull CloudNet nodeInstance) {
    if (Files.notExists(CONFIG_FILE_PATH)) {
      // register the setup if the file does not exists
      nodeInstance.installation().registerSetup(new DefaultConfigSetup());
      return new JsonConfiguration().load();
    } else {
      return JsonDocument.newDocument(CONFIG_FILE_PATH).toInstanceOf(JsonConfiguration.class).load();
    }
  }

  @Override
  public boolean fileExists() {
    return Files.exists(CONFIG_FILE_PATH);
  }

  @Override
  public @NonNull Configuration load() {
    if (this.identity == null) {
      this.identity = new NetworkClusterNode(
        ConfigurationUtils.get(
          "cloudnet.config.identity",
          "Node-" + StringUtil.generateRandomString(4)),
        ConfigurationUtils.get(
          "cloudnet.config.listeners",
          new HostAndPort[]{new HostAndPort(NetworkAddressUtil.localAddress(), 1410)},
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
            var entries = value.split(",");
            for (var entry : entries) {
              // split at '-': <name>-<listeners>
              var info = entry.split("-");
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
        NetworkAddressUtil.availableIPAddresses(),
        value -> Set.of(value.split(",")));
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
        (int) ((ProcessSnapshot.OS_BEAN.getTotalMemorySize() / (1024 * 1024)) - 512),
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

    if (this.forceInitialClusterDataSync == null) {
      this.forceInitialClusterDataSync = ConfigurationUtils.get(
        "cloudnet.config.forceInitialClusterDataSync",
        false,
        Boolean::parseBoolean);
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
      this.hostAddress = ConfigurationUtils.get("cloudnet.config.hostAddress", NetworkAddressUtil.localAddress());
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
          Path.of("cert.pem"),
          Path.of("private.pem")),
        SSL_CONFIG_PARSER);
    }

    if (this.serverSslConfig == null) {
      this.serverSslConfig = ConfigurationUtils.get(
        "cloudnet.config.serverSslConfig",
        new SSLConfiguration(
          false,
          false,
          null,
          Path.of("cert.pem"),
          Path.of("private.pem")),
        SSL_CONFIG_PARSER);
    }

    if (this.webSslConfig == null) {
      this.webSslConfig = ConfigurationUtils.get(
        "cloudnet.config.webSslConfig",
        new SSLConfiguration(
          false,
          false,
          null,
          Path.of("cert.pem"),
          Path.of("private.pem")),
        SSL_CONFIG_PARSER);
    }

    if (this.properties == null) {
      this.properties = ConfigurationUtils.get(
        "cloudnet.config.properties",
        JsonDocument.newDocument("database_provider", "xodus"),
        JsonDocument::fromJsonString);
    }

    return this.save();
  }

  @Override
  public @NonNull Configuration save() {
    JsonDocument.newDocument(this).write(CONFIG_FILE_PATH);
    return this;
  }

  @Override
  public @NonNull NetworkClusterNode identity() {
    return this.identity;
  }

  @Override
  public void identity(@NonNull NetworkClusterNode identity) {
    this.identity = identity;
  }

  @Override
  public @NonNull NetworkCluster clusterConfig() {
    return this.clusterConfig;
  }

  @Override
  public void clusterConfig(@NonNull NetworkCluster clusterConfig) {
    this.clusterConfig = clusterConfig;
  }

  @Override
  public @NonNull Collection<String> ipWhitelist() {
    return this.ipWhitelist != null ? this.ipWhitelist : (this.ipWhitelist = new HashSet<>());
  }

  @Override
  public void ipWhitelist(@NonNull Collection<String> whitelist) {
    this.ipWhitelist = new HashSet<>(whitelist);
  }

  @Override
  public double maxCPUUsageToStartServices() {
    return this.maxCPUUsageToStartServices;
  }

  @Override
  public void maxCPUUsageToStartServices(double value) {
    this.maxCPUUsageToStartServices = value;
  }

  @Override
  public boolean runBlockedServiceStartTryLaterAutomatic() {
    return this.runBlockedServiceStartTryLaterAutomatic;
  }

  @Override
  public void runBlockedServiceStartTryLaterAutomatic(boolean runBlockedServiceStartTryLaterAutomatic) {
    this.runBlockedServiceStartTryLaterAutomatic = runBlockedServiceStartTryLaterAutomatic;
  }

  @Override
  public int maxMemory() {
    return this.maxMemory;
  }

  @Override
  public void maxMemory(int memory) {
    this.maxMemory = memory;
  }

  @Override
  public int maxServiceConsoleLogCacheSize() {
    return this.maxServiceConsoleLogCacheSize;
  }

  @Override
  public void maxServiceConsoleLogCacheSize(int maxServiceConsoleLogCacheSize) {
    this.maxServiceConsoleLogCacheSize = maxServiceConsoleLogCacheSize;
  }

  @Override
  public boolean printErrorStreamLinesFromServices() {
    return this.printErrorStreamLinesFromServices;
  }

  @Override
  public void printErrorStreamLinesFromServices(boolean printErrorStreamLinesFromServices) {
    this.printErrorStreamLinesFromServices = printErrorStreamLinesFromServices;
  }

  @Override
  public @NonNull DefaultJVMFlags defaultJVMFlags() {
    return this.defaultJVMFlags;
  }

  @Override
  public void defaultJVMFlags(@NonNull DefaultJVMFlags defaultJVMFlags) {
    this.defaultJVMFlags = defaultJVMFlags;
  }

  @Override
  public @NonNull String hostAddress() {
    return this.hostAddress;
  }

  @Override
  public void hostAddress(@NonNull String hostAddress) {
    this.hostAddress = hostAddress;
  }

  @Override
  public @NonNull Collection<HostAndPort> httpListeners() {
    return this.httpListeners != null ? this.httpListeners : (this.httpListeners = new ArrayList<>());
  }

  @Override
  public void httpListeners(@NonNull Collection<HostAndPort> httpListeners) {
    this.httpListeners = httpListeners;
  }

  @Override
  public @NonNull String connectHostAddress() {
    return this.connectHostAddress;
  }

  @Override
  public void connectHostAddress(@NonNull String connectHostAddress) {
    this.connectHostAddress = connectHostAddress;
  }

  @Override
  public @NonNull SSLConfiguration clientSSLConfig() {
    return this.clientSslConfig;
  }

  @Override
  public void clientSSLConfig(@NonNull SSLConfiguration clientSslConfig) {
    this.clientSslConfig = clientSslConfig;
  }

  @Override
  public @NonNull SSLConfiguration serverSSLConfig() {
    return this.serverSslConfig;
  }

  @Override
  public void serverSSLConfig(@NonNull SSLConfiguration serverSslConfig) {
    this.serverSslConfig = serverSslConfig;
  }

  @Override
  public @NonNull SSLConfiguration webSSLConfig() {
    return this.webSslConfig;
  }

  @Override
  public void webSSLConfig(@NonNull SSLConfiguration webSslConfig) {
    this.webSslConfig = webSslConfig;
  }

  @Override
  public @NonNull String javaCommand() {
    return this.jvmCommand;
  }

  @Override
  public void javaCommand(@NonNull String jvmCommand) {
    this.jvmCommand = jvmCommand;
  }

  @Override
  public int processTerminationTimeoutSeconds() {
    return Math.max(1, this.processTerminationTimeoutSeconds);
  }

  @Override
  public void processTerminationTimeoutSeconds(int processTerminationTimeoutSeconds) {
    this.processTerminationTimeoutSeconds = processTerminationTimeoutSeconds;
  }

  @Override
  public boolean forceInitialClusterDataSync() {
    return this.forceInitialClusterDataSync;
  }

  @Override
  public void forceInitialClusterDataSync(boolean forceInitialClusterDataSync) {
    this.forceInitialClusterDataSync = forceInitialClusterDataSync;
  }

  @Override
  public @NonNull JsonDocument properties() {
    return this.properties;
  }

  @Override
  public void properties(@NonNull JsonDocument properties) {
    this.properties = properties;
  }
}
