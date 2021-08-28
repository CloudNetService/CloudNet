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

import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A snapshot of a process in the Cloud which provides information about the cpu and memory usage, the running threads
 * and the pid.
 */
@ToString
@EqualsAndHashCode
public class ProcessSnapshot {

  private static final ProcessSnapshot EMPTY = new ProcessSnapshot(
    -1, -1, -1, -1, -1, -1, Collections.emptyList(), -1, -1);

  private static final int OWN_PID;

  static {
    String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
    int index = runtimeName.indexOf('@');

    int parsed = -1;
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
  private final long maxHeapMemory;
  private final long heapUsageMemory;
  private final long noHeapUsageMemory;

  private final long unloadedClassCount;
  private final long totalLoadedClassCount;
  private final int currentLoadedClassCount;

  private final Collection<ThreadSnapshot> threads;

  @Deprecated
  public ProcessSnapshot(
    long heapUsageMemory,
    long noHeapUsageMemory,
    long maxHeapMemory,
    int currentLoadedClassCount,
    long totalLoadedClassCount,
    long unloadedClassCount,
    Collection<ThreadSnapshot> threads,
    double cpuUsage,
    int pid
  ) {
    this(
      pid,
      cpuUsage,
      maxHeapMemory,
      heapUsageMemory,
      noHeapUsageMemory,
      unloadedClassCount,
      totalLoadedClassCount,
      currentLoadedClassCount,
      threads);
  }

  public ProcessSnapshot(
    int pid,
    double cpuUsage,
    long maxHeapMemory,
    long heapUsageMemory,
    long noHeapUsageMemory,
    long unloadedClassCount,
    long totalLoadedClassCount,
    int currentLoadedClassCount,
    Collection<ThreadSnapshot> threads
  ) {
    this.pid = pid;
    this.cpuUsage = cpuUsage;
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
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

    return new ProcessSnapshot(
      getOwnPID(),
      CPUUsageResolver.getProcessCPUUsage(),
      memoryMXBean.getHeapMemoryUsage().getMax(),
      memoryMXBean.getHeapMemoryUsage().getUsed(),
      memoryMXBean.getNonHeapMemoryUsage().getUsed(),
      classLoadingMXBean.getUnloadedClassCount(),
      classLoadingMXBean.getTotalLoadedClassCount(),
      classLoadingMXBean.getLoadedClassCount(),
      Thread.getAllStackTraces().keySet().stream().map(ThreadSnapshot::new).collect(Collectors.toList())
    );
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

  public Collection<ThreadSnapshot> getThreads() {
    return this.threads;
  }

  public double getCpuUsage() {
    return this.cpuUsage;
  }

  public int getPid() {
    return this.pid;
  }
}
