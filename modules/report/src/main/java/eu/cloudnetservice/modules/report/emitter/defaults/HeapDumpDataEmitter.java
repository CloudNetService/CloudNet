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

package eu.cloudnetservice.modules.report.emitter.defaults;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.modules.report.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.emitter.ReportDataWriter;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;
import javax.management.JMX;
import javax.management.ObjectName;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public final class HeapDumpDataEmitter implements ReportDataEmitter {

  // https://regex101.com/r/yAG1eb/1
  private static final Pattern LINE_FORMAT = Pattern.compile("^\\s*(\\d+):\\s*(\\d+)\\s*(\\d+)\\s*(\\S+).*$");
  private static final RowBasedFormatter<HeapDumpEntry> ENTRY_FORMATTER = RowBasedFormatter.<HeapDumpEntry>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Order", "Type", "Instances", "Bytes").build())
    .column(HeapDumpEntry::order)
    .column(HeapDumpEntry::type)
    .column(HeapDumpEntry::instances)
    .column(HeapDumpEntry::bytes)
    .build();

  private static final DiagnosticCommandMBean DIAGNOSTIC_COMMAND_MBEAN;

  static {
    DiagnosticCommandMBean bean;
    try {
      // get the bean server and the name of the bean we want to proxy
      var beanServer = ManagementFactory.getPlatformMBeanServer();
      var diagnosticBeanName = ObjectName.getInstance("com.sun.management:type=DiagnosticCommand");

      // construct the proxy
      bean = JMX.newMBeanProxy(beanServer, diagnosticBeanName, DiagnosticCommandMBean.class);
    } catch (Exception exception) {
      // we should normally not reach here
      bean = args -> "";
    }

    // assign the bean field here
    DIAGNOSTIC_COMMAND_MBEAN = bean;
  }

  @Override
  public @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer) {
    var successfulParsed = 0;
    var classHistogram = DIAGNOSTIC_COMMAND_MBEAN.gcClassHistogram(new String[0]);

    // split the line at the line separator and parse the first 100 entries
    Collection<HeapDumpEntry> entries = new LinkedList<>();
    for (var line : classHistogram.split("\n")) {
      var entry = HeapDumpEntry.parseFromLine(line);
      if (entry != null) {
        // successfully parsed - add the entry
        entries.add(entry);

        // check if we've reached the limit
        if (++successfulParsed >= 100) {
          break;
        }
      }
    }

    // format the entries
    return writer
      .beginSection("Heap Dump (first 100 entries)")
      .appendString(String.join("\n", ENTRY_FORMATTER.format(entries)))
      .endSection();
  }

  public interface DiagnosticCommandMBean {

    @NonNull String gcClassHistogram(@NonNull String[] args);
  }

  private record HeapDumpEntry(@NonNull String type, int order, int instances, long bytes) {

    private static @Nullable HeapDumpEntry parseFromLine(@NonNull String line) {
      var matcher = LINE_FORMAT.matcher(line);
      if (matcher.matches()) {
        // the line matches, parse the number types
        var order = Ints.tryParse(matcher.group(1));
        var instances = Ints.tryParse(matcher.group(2));
        var bytes = Longs.tryParse(matcher.group(3));

        // check if we were able to parse all 3 entries
        if (order != null && instances != null && bytes != null) {
          // get a nice type name for the type of the entry
          var type = formatTypeName(matcher.group(4));
          return new HeapDumpEntry(type, order, instances, bytes);
        }
      }

      // unrelated line
      return null;
    }

    private static @NonNull String formatTypeName(@NonNull String type) {
      try {
        // try to format the type, this does not work for objects as they are not in a descriptor form
        return Type.getType(type).getClassName();
      } catch (IllegalArgumentException exception) {
        // yep, already formatted
        return type;
      }
    }
  }
}
