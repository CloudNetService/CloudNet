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
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
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
public class ProcessSnapshot implements SerializableObject {

  private static final ProcessSnapshot EMPTY = new ProcessSnapshot(-1, -1, -1, -1, -1, -1, Collections.emptyList(), -1,
    -1);

  private static final int ownPID;

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

    ownPID = parsed;
  }

  private long heapUsageMemory;
  private long noHeapUsageMemory;
  private long maxHeapMemory;

  private int currentLoadedClassCount;

  private long totalLoadedClassCount;
  private long unloadedClassCount;

  private Collection<ThreadSnapshot> threads;

  private double cpuUsage;

  private int pid;

  public ProcessSnapshot(long heapUsageMemory, long noHeapUsageMemory, long maxHeapMemory, int currentLoadedClassCount,
    long totalLoadedClassCount, long unloadedClassCount, Collection<ThreadSnapshot> threads, double cpuUsage, int pid) {
    this.heapUsageMemory = heapUsageMemory;
    this.noHeapUsageMemory = noHeapUsageMemory;
    this.maxHeapMemory = maxHeapMemory;
    this.currentLoadedClassCount = currentLoadedClassCount;
    this.totalLoadedClassCount = totalLoadedClassCount;
    this.unloadedClassCount = unloadedClassCount;
    this.threads = threads;
    this.cpuUsage = cpuUsage;
    this.pid = pid;
  }

  public ProcessSnapshot() {
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
      memoryMXBean.getHeapMemoryUsage().getUsed(),
      memoryMXBean.getNonHeapMemoryUsage().getUsed(),
      memoryMXBean.getHeapMemoryUsage().getMax(),
      classLoadingMXBean.getLoadedClassCount(),
      classLoadingMXBean.getTotalLoadedClassCount(),
      classLoadingMXBean.getUnloadedClassCount(),
      Thread.getAllStackTraces().keySet()
        .stream().map(
        thread -> new ThreadSnapshot(thread.getId(), thread.getName(), thread.getState(), thread.isDaemon(),
          thread.getPriority()))
        .collect(Collectors.toList()),
      CPUUsageResolver.getProcessCPUUsage(),
      getOwnPID()
    );
  }

  /**
   * Gets the PID of the current process or -1 if it couldn't be fetched.
   */
  public static int getOwnPID() {
    return ProcessSnapshot.ownPID;
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

  @Override
  public void write(@NotNull ProtocolBuffer buffer) {
    buffer.writeLong(this.heapUsageMemory);
    buffer.writeLong(this.noHeapUsageMemory);
    buffer.writeLong(this.maxHeapMemory);
    buffer.writeInt(this.currentLoadedClassCount);
    buffer.writeLong(this.totalLoadedClassCount);
    buffer.writeLong(this.unloadedClassCount);
    buffer.writeObjectCollection(this.threads);
    buffer.writeDouble(this.cpuUsage);
    buffer.writeInt(this.pid);
  }

  @Override
  public void read(@NotNull ProtocolBuffer buffer) {
    this.heapUsageMemory = buffer.readLong();
    this.noHeapUsageMemory = buffer.readLong();
    this.maxHeapMemory = buffer.readLong();
    this.currentLoadedClassCount = buffer.readInt();
    this.totalLoadedClassCount = buffer.readLong();
    this.unloadedClassCount = buffer.readLong();
    this.threads = buffer.readObjectCollection(ThreadSnapshot.class);
    this.cpuUsage = buffer.readDouble();
    this.pid = buffer.readInt();
  }
}
