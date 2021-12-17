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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface IConfiguration {

  boolean isFileExists();

  @NotNull IConfiguration load();

  @NotNull IConfiguration save();

  @NotNull String hostAddress();

  void hostAddress(@NotNull String hostAddress);

  @NotNull String connectHostAddress();

  void connectHostAddress(@NotNull String connectHostAddress);

  @NotNull NetworkClusterNode identity();

  void identity(@NotNull NetworkClusterNode identity);

  @NotNull NetworkCluster clusterConfig();

  void clusterConfig(@NotNull NetworkCluster clusterConfig);

  @NotNull
  Collection<String> ipWhitelist();

  void ipWhitelist(@NotNull Collection<String> whitelist);

  @NotNull
  Collection<HostAndPort> httpListeners();

  void httpListeners(@NotNull Collection<HostAndPort> httpListeners);

  @NotNull
  SSLConfiguration clientSSLConfig();

  void clientSSLConfig(@NotNull SSLConfiguration clientSslConfig);

  @NotNull
  SSLConfiguration serverSSLConfig();

  void serverSSLConfig(@NotNull SSLConfiguration serverSslConfig);

  @NotNull
  SSLConfiguration webSSLConfig();

  void webSSLConfig(@NotNull SSLConfiguration webSslConfig);

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

  @NotNull DefaultJVMFlags defaultJVMFlags();

  void defaultJVMFlags(@NotNull DefaultJVMFlags defaultJVMFlags);

  @NotNull String javaCommand();

  void javaCommand(@NotNull String javaCommand);

  int processTerminationTimeoutSeconds();

  void processTerminationTimeoutSeconds(int processTerminationTimeoutSeconds);

  boolean forceInitialClusterDataSync();

  void forceInitialClusterDataSync(boolean forceInitialClusterDataSync);

  @NotNull JsonDocument properties();

  void properties(@NotNull JsonDocument properties);

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
