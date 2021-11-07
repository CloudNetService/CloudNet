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

package eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.node;

import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.ReportDataEmitter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class NodeAllocationEmitter implements ReportDataEmitter<NetworkClusterNodeInfoSnapshot> {

  @Override
  public void emitData(StringBuilder builder, NetworkClusterNodeInfoSnapshot context) {
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    builder.append("CPU usage: (P/S) ")
      .append(CPUUsageResolver.FORMAT.format(CPUUsageResolver.getProcessCPUUsage()))
      .append("/")
      .append(CPUUsageResolver.FORMAT.format(CPUUsageResolver.getSystemCPUUsage()))
      .append("/100%")
      .append("\n")
      .append("Node services memory allocation (U/R/M): ")
      .append(context.getUsedMemory())
      .append("/")
      .append(context.getReservedMemory())
      .append("/")
      .append(context.getMaxMemory())
      .append(" MB")
      .append("\n")
      .append("Heap usage: ")
      .append(memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024))
      .append("/")
      .append(memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024))
      .append("MB")
      .append("\n\n");
  }
}
