/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.service;

import com.sun.management.OperatingSystemMXBean;
import eu.cloudnetservice.common.unsafe.CPUUsageResolver;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;

/**
 * A snapshot of the process resources at a specific time. It holds the most useful information for displaying or
 * storing statistics about a service / node (or anything else).
 *
 * @param pid                     the process id of the component which created the snapshot.
 * @param cpuUsage                the recent usage (in percent) of the cpu usage associated with the component process.
 * @param systemCpuUsage          the recent usage (in percent) of the hosts' system cpu the process is running on.
 * @param maxHeapMemory           the maximum heap memory space the associated process is allowed to use.
 * @param heapUsageMemory         the heap memory of all pools which the associated process is currently using.
 * @param noHeapUsageMemory       the off-heap memory of all pools which the associated process is currently using.
 * @param unloadedClassCount      the amount of classes the associated process unloaded since starting.
 * @param totalLoadedClassCount   the amount of classes which were loaded since the associated process was started.
 * @param currentLoadedClassCount the amount of classes which are currently loaded by the associated process.
 * @param threads                 a snapshot of all threads which are currently known to the associated process.
 * @since 4.0
 */
public record ProcessSnapshot(
  long pid,
  double cpuUsage,
  double systemCpuUsage,
  long maxHeapMemory,
  long heapUsageMemory,
  long noHeapUsageMemory,
  long unloadedClassCount,
  long totalLoadedClassCount,
  int currentLoadedClassCount,
  @NonNull Collection<ThreadSnapshot> threads
) implements Cloneable {

  // init them here to reduce lookup load as the get calls will trigger a full re-scan for the bean
  public static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();
  public static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
  public static final ClassLoadingMXBean CLASS_LOADING_MX_BEAN = ManagementFactory.getClassLoadingMXBean();
  public static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

  private static final long OWN_PID = ProcessHandle.current().pid();
  private static final ProcessSnapshot EMPTY = new ProcessSnapshot(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, Set.of());

  /**
   * Get a jvm static process snapshot which holds no information about any process.
   *
   * @return an empty static process snapshot.
   */
  public static @NonNull ProcessSnapshot empty() {
    return EMPTY;
  }

  /**
   * Creates a new process snapshot info filled with information about the current process.
   *
   * @return a process snapshot holding information about the current process.
   */
  public static @NonNull ProcessSnapshot self() {
    return new ProcessSnapshot(
      ownPID(),
      CPUUsageResolver.processCPUUsage(),
      CPUUsageResolver.systemCPUUsage(),
      MEMORY_MX_BEAN.getHeapMemoryUsage().getMax(),
      MEMORY_MX_BEAN.getHeapMemoryUsage().getUsed(),
      MEMORY_MX_BEAN.getNonHeapMemoryUsage().getUsed(),
      CLASS_LOADING_MX_BEAN.getUnloadedClassCount(),
      CLASS_LOADING_MX_BEAN.getTotalLoadedClassCount(),
      CLASS_LOADING_MX_BEAN.getLoadedClassCount(),
      Arrays.stream(THREAD_MX_BEAN.dumpAllThreads(false, false, 0)).map(ThreadSnapshot::from).toList());
  }

  /**
   * Gets the jvm static, one-time initialized id of the current process.
   *
   * @return the current process id.
   */
  public static long ownPID() {
    return ProcessSnapshot.OWN_PID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ProcessSnapshot clone() {
    try {
      return (ProcessSnapshot) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException();
    }
  }
}
