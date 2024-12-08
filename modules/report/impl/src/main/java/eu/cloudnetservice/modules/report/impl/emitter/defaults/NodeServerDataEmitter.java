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

package eu.cloudnetservice.modules.report.impl.emitter.defaults;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataWriter;
import eu.cloudnetservice.modules.report.impl.emitter.SpecificReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.util.ReportConstants;
import eu.cloudnetservice.node.cluster.LocalNodeServer;
import eu.cloudnetservice.node.cluster.NodeServer;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.utils.base.resource.ResourceFormatter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.NonNull;

@Singleton
@AutoService(services = ReportDataEmitter.class, name = "NodeServer")
public final class NodeServerDataEmitter extends SpecificReportDataEmitter<NodeServer> {

  private final NodeServerProvider nodeServerProvider;

  @Inject
  public NodeServerDataEmitter(@NonNull NodeServerProvider nodeServerProvider) {
    super((writer, nodes) -> writer.appendString("Node Servers (").appendInt(nodes.size()).appendString("):"));
    this.nodeServerProvider = nodeServerProvider;
  }

  @Override
  public @NonNull Class<NodeServer> emittingType() {
    return NodeServer.class;
  }

  @Override
  public @NonNull Collection<NodeServer> collectData() {
    return this.nodeServerProvider.nodeServers();
  }

  @Override
  public @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer, @NonNull NodeServer value) {
    // information about the node which is always present
    writer = writer
      .beginSection(titleWriter -> titleWriter
        .appendString(value.name())
        .appendString(" ")
        .appendString(value.state().name())
        .appendString(value.head() ? " (head)" : "")
        .appendString(value instanceof LocalNodeServer ? " current" : " remote"))
      .appendString("Last State Change: ")
      .appendTimestamp(ReportConstants.DATE_TIME_FORMATTER, value.lastStateChange())
      .appendNewline()
      .appendString("Listeners: ")
      .appendString(value.info().listeners().stream().map(HostAndPort::toString).collect(Collectors.joining(", ")));

    // information which is only present if the node is connected
    var snapshot = value.nodeInfoSnapshot();
    if (snapshot != null) {
      writer = writer
        .beginSection(title -> title.appendString("NodeSnapshot").appendString(snapshot.draining() ? " (drain)" : ""))
        // Startup Time: 27.06.2022 15:41:20; Last Update: 27.06.2022 15:42:12
        .appendString("Startup Time: ")
        .appendTimestamp(ReportConstants.DATE_TIME_FORMATTER, snapshot.startupMillis())
        .appendString("; Last Update: ")
        .appendTimestamp(ReportConstants.DATE_TIME_FORMATTER, snapshot.creationTime())
        .appendNewline()
        // Memory: 256 MB used; 1280 MB reserved; 6094 MB max; 2 services
        .appendString("Memory: ")
        .appendInt(snapshot.usedMemory())
        .appendString(" MB used; ")
        .appendInt(snapshot.reservedMemory())
        .appendString(" MB reserved; ")
        .appendInt(snapshot.maxMemory())
        .appendString(" MB max; ")
        .appendInt(snapshot.currentServicesCount())
        .appendString(" services")
        .appendNewline()
        // CPU: 0% process; 0.57% system
        .appendString("CPU: ")
        .appendString(ResourceFormatter.formatTwoDigitPrecision(snapshot.processSnapshot().cpuUsage()))
        .appendString("% process; ")
        .appendString(ResourceFormatter.formatTwoDigitPrecision(snapshot.processSnapshot().systemCpuUsage()))
        .appendString("% system")
        .appendNewline()
        // Heap-Memory: 54525952 MB used; 268435456 MB max
        .appendString("Heap-Memory: ")
        .appendLong(snapshot.processSnapshot().heapUsageMemory())
        .appendString(" MB used; ")
        .appendLong(snapshot.processSnapshot().maxHeapMemory())
        .appendString(" MB max");

      // Modules (10):
      var modules = snapshot.modules();
      writer = writer
        .beginSection(title -> title.appendString("Modules (").appendInt(modules.size()).appendString("):"));

      for (var module : modules) {
        writer
          // - eu.cloudnetservice.cloudnet; CloudNet-Influx; 4.0.0-SNAPSHOT by CloudNetService
          .appendString("- ")
          .appendString(module.group())
          .appendString("; ")
          .appendString(module.name())
          .appendString("; ")
          .appendString(module.version())
          .appendString(" by ")
          .appendString(module.author() == null ? "<unknown>" : module.author())
          .appendNewline();
      }

      // end the module and snapshot section
      writer = writer.endSection().endSection();
    }

    // finish the section
    return writer.endSection();
  }
}
