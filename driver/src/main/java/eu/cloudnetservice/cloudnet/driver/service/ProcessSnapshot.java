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

package eu.cloudnetservice.cloudnet.driver.service;

import com.sun.management.OperatingSystemMXBean;
import eu.cloudnetservice.cloudnet.common.unsafe.CPUUsageResolver;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Collection;
import java.util.Collections;
import lombok.NonNull;

/**
 * A snapshot of a process in the Cloud which provides information about the cpu and memory usage, the running threads
 * and the pid.
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

  // init them here to reduce lookup load
  public static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();
  public static final ClassLoadingMXBean CLASS_LOADING_MX_BEAN = ManagementFactory.getClassLoadingMXBean();
  public static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

  private static final long OWN_PID = ProcessHandle.current().pid();
  private static final ProcessSnapshot EMPTY = new ProcessSnapshot(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, Collections.emptyList());

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
      ownPID(),
      CPUUsageResolver.processCPUUsage(),
      CPUUsageResolver.systemCPUUsage(),
      MEMORY_MX_BEAN.getHeapMemoryUsage().getMax(),
      MEMORY_MX_BEAN.getHeapMemoryUsage().getUsed(),
      MEMORY_MX_BEAN.getNonHeapMemoryUsage().getUsed(),
      CLASS_LOADING_MX_BEAN.getUnloadedClassCount(),
      CLASS_LOADING_MX_BEAN.getTotalLoadedClassCount(),
      CLASS_LOADING_MX_BEAN.getLoadedClassCount(),
      Thread.getAllStackTraces().keySet().stream().map(ThreadSnapshot::from).toList());
  }

  /**
   * Gets the PID of the current process or -1 if it couldn't be fetched.
   */
  public static long ownPID() {
    return ProcessSnapshot.OWN_PID;
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
