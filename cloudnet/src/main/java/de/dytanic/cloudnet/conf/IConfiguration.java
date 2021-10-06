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

package de.dytanic.cloudnet.conf;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkCluster;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.ssl.SSLConfiguration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface IConfiguration {

  boolean isFileExists();

  void load();

  void save();

  void setDefaultHostAddress(String hostAddress);

  String getDefaultHostAddress();

  String getHostAddress();

  void setHostAddress(String hostAddress);

  String getConnectHostAddress();

  void setConnectHostAddress(String connectHostAddress);

  NetworkClusterNode getIdentity();

  void setIdentity(NetworkClusterNode identity);

  NetworkCluster getClusterConfig();

  void setClusterConfig(@NotNull NetworkCluster clusterConfig);

  @NotNull
  Collection<String> getIpWhitelist();

  void setIpWhitelist(@NotNull Collection<String> whitelist);

  @NotNull
  Collection<HostAndPort> getHttpListeners();

  void setHttpListeners(@NotNull Collection<HostAndPort> httpListeners);

  @NotNull
  SSLConfiguration getClientSslConfig();

  @NotNull
  SSLConfiguration getServerSslConfig();

  @NotNull
  SSLConfiguration getWebSslConfig();

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

  DefaultJVMFlags getDefaultJVMFlags();

  void setDefaultJVMFlags(DefaultJVMFlags defaultJVMFlags);

  String getJVMCommand();

  int getProcessTerminationTimeoutSeconds();

  void setProcessTerminationTimeoutSeconds(int processTerminationTimeoutSeconds);

  enum DefaultJVMFlags {
    NONE(Collections.emptyList()),
    DYTANIC(Arrays.asList(
      "-XX:+UseG1GC",
      "-XX:MaxGCPauseMillis=50",
      "-XX:-UseAdaptiveSizePolicy",
      "-XX:CompileThreshold=100",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+UseCompressedOops"
    )),
    // https://aikar.co/2018/07/02/tuning-the-jvm-g1gc-garbage-collector-flags-for-minecraft/
    AIKAR(Arrays.asList(
      "-XX:+UseG1GC",
      "-XX:+ParallelRefProcEnabled",
      "-XX:MaxGCPauseMillis=200",
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+DisableExplicitGC",
      "-XX:+AlwaysPreTouch",
      "-XX:G1NewSizePercent=30",
      "-XX:G1MaxNewSizePercent=40",
      "-XX:G1HeapRegionSize=8M",
      "-XX:G1ReservePercent=20",
      "-XX:G1HeapWastePercent=5",
      "-XX:G1MixedGCCountTarget=4",
      "-XX:InitiatingHeapOccupancyPercent=15",
      "-XX:G1MixedGCLiveThresholdPercent=90",
      "-XX:G1RSetUpdatingPauseTimePercent=5",
      "-XX:SurvivorRatio=32",
      "-XX:+PerfDisableSharedMem",
      "-XX:MaxTenuringThreshold=1",
      "-Dusing.aikars.flags=https://mcflags.emc.gs",
      "-Daikars.new.flags=true"
    ));

    private final List<String> jvmFlags;

    DefaultJVMFlags(List<String> jvmFlags) {
      this.jvmFlags = jvmFlags;
    }

    public List<String> getJvmFlags() {
      return this.jvmFlags;
    }

  }

}
