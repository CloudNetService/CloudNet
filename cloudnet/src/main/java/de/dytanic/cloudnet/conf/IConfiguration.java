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

    void setHostAddress(String hostAddress);

    NetworkClusterNode getIdentity();

    void setIdentity(NetworkClusterNode identity);

    NetworkCluster getClusterConfig();

    void setClusterConfig(NetworkCluster clusterConfig);

    Collection<String> getIpWhitelist();

    void setIpWhitelist(Collection<String> whitelist);

    Collection<HostAndPort> getHttpListeners();

    void setHttpListeners(Collection<HostAndPort> httpListeners);

    ConfigurationOptionSSL getClientSslConfig();

    ConfigurationOptionSSL getServerSslConfig();

    ConfigurationOptionSSL getWebSslConfig();

    double getMaxCPUUsageToStartServices();

    void setMaxCPUUsageToStartServices(double value);

    int getMaxMemory();

    void setMaxMemory(int memory);

    int getMaxServiceConsoleLogCacheSize();

    void setMaxServiceConsoleLogCacheSize(int maxServiceConsoleLogCacheSize);

    boolean isPrintErrorStreamLinesFromServices();

    void setPrintErrorStreamLinesFromServices(boolean printErrorStreamLinesFromServices);

    boolean isParallelServiceStartSequence();

    void setParallelServiceStartSequence(boolean value);

    boolean isRunBlockedServiceStartTryLaterAutomatic();

    void setRunBlockedServiceStartTryLaterAutomatic(boolean runBlockedServiceStartTryLaterAutomatic);

    boolean isDefaultJVMOptionParameters();

    void setDefaultJVMOptionParameters(boolean value);

    String getJVMCommand();

}