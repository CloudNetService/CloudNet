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

package de.dytanic.cloudnet.common.unsafe;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class includes the methods to getDef the CPU load exactly from the com.sun.management.OperatingSystemMXBean
 * class and to encapsulate the operations from the unsafe access com.sun.management.* classes
 *
 * @see com.sun.management.OperatingSystemMXBean
 */
@ApiStatus.Internal
public final class CPUUsageResolver {

  /**
   * A simple decimal format to easy display the CPU usage value.
   */
  public static final DecimalFormat CPU_USAGE_OUTPUT_FORMAT = new DecimalFormat("##.##");

  //default initialization
  static {
    getProcessCPUUsage();
    getSystemCPUUsage();
    getSystemMemory();
  }

  private CPUUsageResolver() {
    throw new UnsupportedOperationException();
  }

  /**
   * For encapsulation and easy operation of the operating systemMXBean its getCpuLoad() method.
   *
   * @return the current system cpu usage as double value
   * @see com.sun.management.OperatingSystemMXBean
   */
  public static double getSystemCPUUsage() {
    return toPercentage(getSystemMxBean().getSystemCpuLoad());
  }

  /**
   * For encapsulation and easy operation of the operating systemMXBean its getCpuLoad() method.
   *
   * @return the cpu usage of this JVM process as double value in range from 0 to 100 % in percent
   * @see com.sun.management.OperatingSystemMXBean
   */
  public static double getProcessCPUUsage() {
    return toPercentage(getSystemMxBean().getProcessCpuLoad());
  }

  /**
   * For encapsulation and easy operation of the operating systemMXBean its getCpuLoad() method.
   *
   * @return the system configured memory in bytes
   * @see com.sun.management.OperatingSystemMXBean
   */
  public static long getSystemMemory() {
    return getSystemMxBean().getTotalPhysicalMemorySize();
  }

  private static double toPercentage(double input) {
    return input < 0 ? -1 : input * 100;
  }

  private static OperatingSystemMXBean getSystemMxBean() {
    return (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
  }
}
