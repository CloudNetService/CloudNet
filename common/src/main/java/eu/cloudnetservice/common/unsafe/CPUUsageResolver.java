/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.common.unsafe;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class includes the methods to getDef the CPU load exactly from the com.sun.management.OperatingSystemMXBean
 * class and to encapsulate the operations from the unsafe access com.sun.management.* classes
 *
 * @see com.sun.management.OperatingSystemMXBean
 */
@ApiStatus.Internal
public final class CPUUsageResolver {

  public static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
  private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(
    () -> new DecimalFormat("##.##"));

  private CPUUsageResolver() {
    throw new UnsupportedOperationException();
  }

  /**
   * For encapsulation and easy operation of the operating systemMXBean its getSystemCpuLoad() method.
   *
   * @return the current system cpu usage as double value
   * @see com.sun.management.OperatingSystemMXBean
   */
  public static double systemCPUUsage() {
    return toPercentage(OS_BEAN.getCpuLoad());
  }

  /**
   * For encapsulation and easy operation of the operating systemMXBean its getProcessCpuLoad() method.
   *
   * @return the cpu usage of this JVM process as double value in range from 0 to 100 % in percent
   * @see com.sun.management.OperatingSystemMXBean
   */
  public static double processCPUUsage() {
    return toPercentage(OS_BEAN.getProcessCpuLoad());
  }

  /**
   * Gets a decimal format with the pattern {@code ##.##} which is safe to be used on the current thread. Each thread
   * accessing this method gets a different decimal format instance to ensure thread safety.
   *
   * @return a thread-local decimal format instance.
   */
  public static @NonNull DecimalFormat defaultFormat() {
    return DECIMAL_FORMAT.get();
  }

  private static double toPercentage(double input) {
    return input < 0 ? -1 : input * 100;
  }
}
