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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import java.util.Collection;
import lombok.NonNull;

public interface Configuration {

  boolean fileExists();

  @NonNull Configuration load();

  @NonNull Configuration save();

  @NonNull String hostAddress();

  void hostAddress(@NonNull String hostAddress);

  @NonNull String connectHostAddress();

  void connectHostAddress(@NonNull String connectHostAddress);

  @NonNull NetworkClusterNode identity();

  void identity(@NonNull NetworkClusterNode identity);

  @NonNull NetworkCluster clusterConfig();

  void clusterConfig(@NonNull NetworkCluster clusterConfig);

  @NonNull
  Collection<String> ipWhitelist();

  void ipWhitelist(@NonNull Collection<String> whitelist);

  @NonNull
  Collection<HostAndPort> httpListeners();

  void httpListeners(@NonNull Collection<HostAndPort> httpListeners);

  @NonNull
  SSLConfiguration clientSSLConfig();

  void clientSSLConfig(@NonNull SSLConfiguration clientSslConfig);

  @NonNull
  SSLConfiguration serverSSLConfig();

  void serverSSLConfig(@NonNull SSLConfiguration serverSslConfig);

  @NonNull
  SSLConfiguration webSSLConfig();

  void webSSLConfig(@NonNull SSLConfiguration webSslConfig);

  double maxCPUUsageToStartServices();

  void maxCPUUsageToStartServices(double value);

  int maxMemory();

  void maxMemory(int memory);

  int maxServiceConsoleLogCacheSize();

  void maxServiceConsoleLogCacheSize(int maxServiceConsoleLogCacheSize);

  boolean printErrorStreamLinesFromServices();

  void printErrorStreamLinesFromServices(boolean printErrorStreamLinesFromServices);

  boolean runBlockedServiceStartTryLaterAutomatic();

  void runBlockedServiceStartTryLaterAutomatic(boolean runBlockedServiceStartTryLaterAutomatic);

  @NonNull String javaCommand();

  void javaCommand(@NonNull String javaCommand);

  int processTerminationTimeoutSeconds();

  void processTerminationTimeoutSeconds(int processTerminationTimeoutSeconds);

  boolean forceInitialClusterDataSync();

  void forceInitialClusterDataSync(boolean forceInitialClusterDataSync);

  @NonNull JsonDocument properties();

  void properties(@NonNull JsonDocument properties);

}
