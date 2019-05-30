package de.dytanic.cloudnet.conf;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;

@Getter
public final class JsonConfiguration implements IConfiguration {

  private static final Type
    CLUSTER_NODE = new TypeToken<NetworkClusterNode>() {
  }.getType(),
    CLUSTER = new TypeToken<NetworkCluster>() {
    }.getType(),
    COLLECTION_STRING = new TypeToken<Collection<String>>() {
    }.getType(),
    HOST_AND_PORT_COLLECTION = new TypeToken<Collection<HostAndPort>>() {
    }.getType();

  private static final Path CONFIG_FILE_PATH = Paths
    .get(System.getProperty("cloudnet.config.json.path", "config.json"));

  private JsonDocument document;

  private NetworkClusterNode identity;

  private NetworkCluster clusterConfig;

  private Collection<String> ipWhitelist;

  private double maxCPUUsageToStartServices;

  private boolean parallelServiceStartSequence, runBlockedServiceStartTryLaterAutomatic;

  private int maxMemory, maxServiceConsoleLogCacheSize;

  private boolean printErrorStreamLinesFromServices, defaultJVMOptionParameters;

  private String hostAddress;

  private Collection<HostAndPort> httpListeners;

  private ConfigurationOptionSSL clientSslConfig, serverSslConfig, webSslConfig;

  private String jVMCommand;

  @Setter
  private String defaultHostAddress;

  @Override
  public boolean isFileExists() {
    return Files.exists(CONFIG_FILE_PATH);
  }

  @Override
  public void load() {
    this.document = JsonDocument.newDocument(CONFIG_FILE_PATH);

    Collection<String> addresses = Iterables.newHashSet();
    addresses.add("127.0.0.1");
    addresses.add("127.0.1.1");

    try {
      Iterables.forEach(NetworkInterface.getNetworkInterfaces(),
        new Consumer<NetworkInterface>() {
          @Override
          public void accept(NetworkInterface networkInterface) {
            Iterables.forEach(networkInterface.getInetAddresses(),
              new Consumer<InetAddress>() {
                @Override
                public void accept(InetAddress inetAddress) {
                  addresses.add(inetAddress.getHostAddress());
                }
              });
          }
        });
    } catch (SocketException ignored) {
    }

    String address = defaultHostAddress;

    if (address == null) {
      try {
        address = InetAddress.getLocalHost().getHostAddress();
      } catch (UnknownHostException e) {
        address = "127.0.0.1";
      }
    }

    addresses.add(address);

    this.identity = this.document
      .get("identity", CLUSTER_NODE, new NetworkClusterNode(
        System.getenv("CLOUDNET_CLUSTER_NODE_UNIQUE_ID") != null ?
          System.getenv("CLOUDNET_CLUSTER_NODE_UNIQUE_ID") :
          "Node-" + UUID.randomUUID().toString().split("-")[0],
        new HostAndPort[]{
          new HostAndPort(address, 1410)
        }
      ));

    if (System.getenv("CLOUDNET_DEFAULT_IP_WHITELIST") != null) {
      addresses.addAll(Arrays
        .asList(System.getenv("CLOUDNET_DEFAULT_IP_WHITELIST").split(",")));
    }

    this.ipWhitelist = this.document
      .get("ipWhitelist", COLLECTION_STRING, addresses);

    this.clusterConfig = this.document
      .get("cluster", CLUSTER, new NetworkCluster(
        System.getenv("CLOUDNET_CLUSTER_ID") != null ?
          UUID.fromString(System.getenv("CLOUDNET_CLUSTER_ID")) :
          UUID.randomUUID(),
        Collections.emptyList()
      ));

    this.maxCPUUsageToStartServices = this.document
      .getDouble("maxCPUUsageToStartServices", 100D);
    this.parallelServiceStartSequence = this.document
      .getBoolean("parallelServiceStartSequence", true);
    this.runBlockedServiceStartTryLaterAutomatic = this.document
      .getBoolean("runBlockedServiceStartTryLaterAutomatic", true);

    this.maxMemory = this.document.getInt("maxMemory",
      (int) ((CPUUsageResolver.getSystemMemory() / 1048576) - 2048));
    this.maxServiceConsoleLogCacheSize = this.document
      .getInt("maxServiceConsoleLogCacheSize", 64);
    this.printErrorStreamLinesFromServices = this.document
      .getBoolean("printErrorStreamLinesFromServices", true);
    this.defaultJVMOptionParameters = this.document
      .getBoolean("defaultJVMOptionParameters", true);

    this.jVMCommand = this.document.getString("jvmCommand",
      System.getenv("CLOUDNET_RUNTIME_JVM_COMMAND") != null ?
        System.getenv("CLOUDNET_RUNTIME_JVM_COMMAND") :
        "java"
    );

    this.hostAddress = this.document.getString("hostAddress", address);
    this.httpListeners = this.document
      .get("httpListeners", HOST_AND_PORT_COLLECTION,
        Collections.singletonList(new HostAndPort("0.0.0.0", 2812)));

    ConfigurationOptionSSL fallback = new ConfigurationOptionSSL(
      false,
      false,
      null,
      "local/certificate.pem",
      "local/privateKey.key"
    );

    this.clientSslConfig = this.document
      .get("clientSslConfig", ConfigurationOptionSSL.class, fallback);
    this.serverSslConfig = this.document
      .get("serverSslConfig", ConfigurationOptionSSL.class, fallback);
    this.webSslConfig = this.document
      .get("webSslConfig", ConfigurationOptionSSL.class, fallback);

    if (System.getProperty("cloudnet.cluster.id") != null) {
      this.clusterConfig.setClusterId(
        UUID.fromString(System.getProperty("cloudnet.cluster.id")));
    }

    this.document.write(CONFIG_FILE_PATH);
  }

  @Override
  public void save() {
    if (document == null) {
      document = new JsonDocument();
    }

    this.document
      .append("identity", this.identity)
      .append("ipWhitelist", this.ipWhitelist)
      .append("maxMemory", this.maxMemory)
      .append("jvmCommand", this.jVMCommand)
      .append("maxServiceConsoleLogCacheSize",
        this.maxServiceConsoleLogCacheSize)
      .append("printErrorStreamLinesFromServices",
        this.printErrorStreamLinesFromServices)
      .append("maxCPUUsageToStartServices", this.maxCPUUsageToStartServices)
      .append("parallelServiceStartSequence",
        this.parallelServiceStartSequence)
      .append("defaultJVMOptionParameters", this.defaultJVMOptionParameters)
      .append("runBlockedServiceStartTryLaterAutomatic",
        this.runBlockedServiceStartTryLaterAutomatic)
      .append("cluster", this.clusterConfig)
      .append("hostAddress", this.hostAddress)
      .append("httpListeners", this.httpListeners)
      .append("clientSslConfig", this.clientSslConfig)
      .append("serverSslConfig", this.serverSslConfig)
      .append("webSslConfig", this.webSslConfig)
      .write(CONFIG_FILE_PATH);
  }

  @Override
  public void setIpWhitelist(Collection<String> whitelist) {
    Validate.checkNotNull(whitelist);

    this.ipWhitelist = whitelist;
    this.save();
  }

  @Override
  public void setClusterConfig(NetworkCluster clusterConfig) {
    Validate.checkNotNull(clusterConfig);

    this.clusterConfig = clusterConfig;
    this.save();
  }

  @Override
  public void setMaxCPUUsageToStartServices(double value) {
    this.maxCPUUsageToStartServices = value;
    this.save();
  }

  @Override
  public void setDefaultJVMOptionParameters(
    boolean defaultJVMOptionParameters) {
    this.defaultJVMOptionParameters = defaultJVMOptionParameters;
    this.save();
  }

  @Override
  public void setPrintErrorStreamLinesFromServices(
    boolean printErrorStreamLinesFromServices) {
    this.printErrorStreamLinesFromServices = printErrorStreamLinesFromServices;
    this.save();
  }

  @Override
  public void setRunBlockedServiceStartTryLaterAutomatic(
    boolean runBlockedServiceStartTryLaterAutomatic) {
    this.runBlockedServiceStartTryLaterAutomatic = runBlockedServiceStartTryLaterAutomatic;
    this.save();
  }

  @Override
  public void setParallelServiceStartSequence(
    boolean parallelServiceStartSequence) {
    this.parallelServiceStartSequence = parallelServiceStartSequence;
    this.save();
  }

  @Override
  public void setMaxMemory(int memory) {
    this.maxMemory = memory;
    this.save();
  }

  @Override
  public void setMaxServiceConsoleLogCacheSize(
    int maxServiceConsoleLogCacheSize) {
    this.maxServiceConsoleLogCacheSize = maxServiceConsoleLogCacheSize;
    this.save();
  }

  @Override
  public void setHttpListeners(Collection<HostAndPort> httpListeners) {
    Validate.checkNotNull(httpListeners);

    this.httpListeners = httpListeners;
    this.save();
  }
}