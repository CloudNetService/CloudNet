package de.dytanic.cloudnet.conf;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import java.util.Collection;

public interface IConfiguration {

  boolean isFileExists();

  void load();

  void save();

  void setDefaultHostAddress(String hostAddress);

  String getHostAddress();

  NetworkClusterNode getIdentity();

  NetworkCluster getClusterConfig();

  Collection<String> getIpWhitelist();

  Collection<HostAndPort> getHttpListeners();

  ConfigurationOptionSSL getClientSslConfig();

  ConfigurationOptionSSL getServerSslConfig();

  ConfigurationOptionSSL getWebSslConfig();

  void setIpWhitelist(Collection<String> whitelist);

  void setClusterConfig(NetworkCluster clusterConfig);

  double getMaxCPUUsageToStartServices();

  void setMaxCPUUsageToStartServices(double value);

  void setPrintErrorStreamLinesFromServices(
    boolean printErrorStreamLinesFromServices);

  int getMaxMemory();

  void setMaxMemory(int memory);

  int getMaxServiceConsoleLogCacheSize();

  boolean isPrintErrorStreamLinesFromServices();

  void setMaxServiceConsoleLogCacheSize(int maxServiceConsoleLogCacheSize);

  boolean isParallelServiceStartSequence();

  boolean isRunBlockedServiceStartTryLaterAutomatic();

  boolean isDefaultJVMOptionParameters();

  void setDefaultJVMOptionParameters(boolean value);

  String getJVMCommand();

  void setRunBlockedServiceStartTryLaterAutomatic(
    boolean runBlockedServiceStartTryLaterAutomatic);

  void setParallelServiceStartSequence(boolean value);

  void setHttpListeners(Collection<HostAndPort> httpListeners);

}