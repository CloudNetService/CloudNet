/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.config;

import eu.cloudnetservice.driver.cluster.NetworkCluster;
import eu.cloudnetservice.driver.cluster.NetworkClusterNode;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.network.ssl.SSLConfiguration;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import lombok.NonNull;

@Singleton
public interface Configuration {

  boolean fileExists();

  @NonNull Configuration load();

  @NonNull Configuration save();

  void reloadFrom(@NonNull Configuration configuration);

  @NonNull String language();

  void language(@NonNull String language);

  @NonNull String hostAddress();

  void hostAddress(@NonNull String hostAddress);

  @NonNull Map<String, String> ipAliases();

  void ipAliases(@NonNull Map<String, String> alias);

  @NonNull NetworkClusterNode identity();

  void identity(@NonNull NetworkClusterNode identity);

  @NonNull NetworkCluster clusterConfig();

  void clusterConfig(@NonNull NetworkCluster clusterConfig);

  @NonNull Collection<String> ipWhitelist();

  void ipWhitelist(@NonNull Collection<String> whitelist);

  @NonNull SSLConfiguration clientSSLConfig();

  void clientSSLConfig(@NonNull SSLConfiguration clientSslConfig);

  @NonNull SSLConfiguration serverSSLConfig();

  void serverSSLConfig(@NonNull SSLConfiguration serverSslConfig);

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

  @NonNull Document properties();

  void properties(@NonNull Document properties);

}
