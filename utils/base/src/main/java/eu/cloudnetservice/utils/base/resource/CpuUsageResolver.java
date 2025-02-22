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

package eu.cloudnetservice.utils.base.resource;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import org.jetbrains.annotations.Range;

/**
 * A utility class to resolve the usage of the host system CPU.
 *
 * @since 4.0
 */
public final class CpuUsageResolver {

  private static final OperatingSystemMXBean OS_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

  private CpuUsageResolver() {
    throw new UnsupportedOperationException();
  }

  /**
   * Reads the current system cpu usage and converts the returned value into a percentage. Note: in case that the value
   * is not available, this method returns {@code -1}.
   *
   * @return the current system cpu load formatted to a percentage or -1 if not available.
   */
  public static @Range(from = -1, to = 100) double systemCpuLoad() {
    return ResourceFormatter.convertToPercentage(OS_BEAN.getCpuLoad());
  }

  /**
   * Reads the process cpu usage for the current jvm process and converts the returned value into a percentage. Note: in
   * case that the value is not available, this method returns {@code -1}.
   *
   * @return the current process cpu load formatted to a percentage or -1 if not available.
   */
  public static @Range(from = -1, to = 100) double processCpuLoad() {
    return ResourceFormatter.convertToPercentage(OS_BEAN.getProcessCpuLoad());
  }
}
