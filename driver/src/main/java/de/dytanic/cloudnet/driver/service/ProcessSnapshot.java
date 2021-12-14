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

package de.dytanic.cloudnet.driver.service;

import com.sun.management.OperatingSystemMXBean;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * A snapshot of a process in the Cloud which provides information about the cpu and memory usage, the running threads
 * and the pid.
 */
@ToString
@EqualsAndHashCode
public class ProcessSnapshot implements Cloneable {

  // init them here to reduce lookup load
  public static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();
  public static final ClassLoadingMXBean CLASS_LOADING_MX_BEAN = ManagementFactory.getClassLoadingMXBean();
  public static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

  private static final int OWN_PID;
  private static final ProcessSnapshot EMPTY = new ProcessSnapshot(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, Collections.emptyList());

  static {
    var runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    var index = runtimeName.indexOf('@');

    var parsed = -1;
    if (index > 0) {
      try {
        parsed = Integer.parseInt(runtimeName.substring(0, index));
      } catch (NumberFormatException ignored) {
      }
    }

    OWN_PID = parsed;
  }

  private final int pid;

  private final double cpuUsage;
  private final double systemCpuUsage;

  private final long maxHeapMemory;
  private final long heapUsageMemory;
  private final long noHeapUsageMemory;

  private final long unloadedClassCount;
  private final long totalLoadedClassCount;
  private final int currentLoadedClassCount;

  private final Collection<ThreadSnapshot> threads;

  protected ProcessSnapshot(
    int pid,
    double cpuUsage,
    double systemCpuUsage,
    long maxHeapMemory,
    long heapUsageMemory,
    long noHeapUsageMemory,
    long unloadedClassCount,
    long totalLoadedClassCount,
    int currentLoadedClassCount,
    @NotNull Collection<ThreadSnapshot> threads
  ) {
    this.pid = pid;
    this.cpuUsage = cpuUsage;
    this.systemCpuUsage = systemCpuUsage;
    this.maxHeapMemory = maxHeapMemory;
    this.heapUsageMemory = heapUsageMemory;
    this.noHeapUsageMemory = noHeapUsageMemory;
    this.unloadedClassCount = unloadedClassCount;
    this.totalLoadedClassCount = totalLoadedClassCount;
    this.currentLoadedClassCount = currentLoadedClassCount;
    this.threads = threads;
  }

  /**
   * Gets an empty snapshot without any information about the process.
   *
   * @return an empty constant {@link ProcessSnapshot}
   */
  public static ProcessSnapshot empty() {
    return EMPTY;
  }

  /**
   * Creates a new snapshot with information about the current process.
   *
   * @return a new {@link ProcessSnapshot}
   */
  public static ProcessSnapshot self() {
    return new ProcessSnapshot(
      getOwnPID(),
      CPUUsageResolver.getProcessCPUUsage(),
      CPUUsageResolver.getSystemCPUUsage(),
      MEMORY_MX_BEAN.getHeapMemoryUsage().getMax(),
      MEMORY_MX_BEAN.getHeapMemoryUsage().getUsed(),
      MEMORY_MX_BEAN.getNonHeapMemoryUsage().getUsed(),
      CLASS_LOADING_MX_BEAN.getUnloadedClassCount(),
      CLASS_LOADING_MX_BEAN.getTotalLoadedClassCount(),
      CLASS_LOADING_MX_BEAN.getLoadedClassCount(),
      Thread.getAllStackTraces().keySet().stream().map(ThreadSnapshot::from).collect(Collectors.toList()));
  }

  /**
   * Gets the PID of the current process or -1 if it couldn't be fetched.
   */
  public static int getOwnPID() {
    return ProcessSnapshot.OWN_PID;
  }

  public long getHeapUsageMemory() {
    return this.heapUsageMemory;
  }

  public long getNoHeapUsageMemory() {
    return this.noHeapUsageMemory;
  }

  public long getMaxHeapMemory() {
    return this.maxHeapMemory;
  }

  public int getCurrentLoadedClassCount() {
    return this.currentLoadedClassCount;
  }

  public long getTotalLoadedClassCount() {
    return this.totalLoadedClassCount;
  }

  public long getUnloadedClassCount() {
    return this.unloadedClassCount;
  }

  public @NotNull Collection<ThreadSnapshot> getThreads() {
    return this.threads;
  }

  public double getCpuUsage() {
    return this.cpuUsage;
  }

  public double getSystemCpuUsage() {
    return this.systemCpuUsage;
  }

  public int getPid() {
    return this.pid;
  }

  @Override
  public ProcessSnapshot clone() {
    try {
      return (ProcessSnapshot) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException();
    }
  }
}
