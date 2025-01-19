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

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataWriter;
import eu.cloudnetservice.utils.base.column.ColumnFormatter;
import eu.cloudnetservice.utils.base.column.RowedFormatter;
import jakarta.inject.Singleton;
import java.lang.constant.ClassDesc;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;
import javax.management.JMX;
import javax.management.ObjectName;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@AutoService(services = ReportDataEmitter.class, name = "HeapDump")
public final class HeapDumpDataEmitter implements ReportDataEmitter {

  // https://regex101.com/r/yAG1eb/1
  private static final Pattern LINE_FORMAT = Pattern.compile("^\\s*(\\d+):\\s*(\\d+)\\s*(\\d+)\\s*(\\S+).*$");
  private static final RowedFormatter<HeapDumpEntry> ENTRY_FORMATTER = RowedFormatter.<HeapDumpEntry>builder()
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

    @NonNull
    String gcClassHistogram(@NonNull String[] args);
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
          var type = prettyPrintDescriptor(matcher.group(4));
          return new HeapDumpEntry(type, order, instances, bytes);
        }
      }

      // unrelated line
      return null;
    }

    private static @NonNull String prettyPrintDescriptor(@NonNull String descriptor) {
      try {
        var parseableDescriptor = descriptor;
        if (parseableDescriptor.contains(".")) {
          // some class descriptors seem to be somewhat pre-formatted, but not quite...
          // the package name already contains dots, but the name is still prefixed with
          // descriptor elements, like: "[Ljdk.internal.vm.FillerElement;"
          parseableDescriptor = parseableDescriptor.replace('.', '/');
        }

        var parsedDescriptor = ClassDesc.ofDescriptor(parseableDescriptor);
        return prettyPrintClassDesc(parsedDescriptor);
      } catch (IllegalArgumentException ignored) {
        // type is not a valid descriptor, assume it's already a binary class name
        return descriptor;
      }
    }

    private static @NonNull String prettyPrintClassDesc(@NonNull ClassDesc classDesc) {
      if (classDesc.isArray()) {
        // pretty print the component type of the array and append a [] suffix to indicate the array
        var componentType = classDesc.componentType();
        var prettyPrintedComponentType = prettyPrintClassDesc(componentType);
        return prettyPrintedComponentType + "[]";
      }

      if (classDesc.isPrimitive()) {
        // just display the name of the primitive type
        return classDesc.displayName();
      } else {
        var packageName = classDesc.packageName();
        var className = classDesc.displayName();
        if (packageName.isBlank()) {
          // class is located in unnamed package, just return the class name
          return className;
        } else {
          // class is in named, prefix the class name with the package name
          return packageName + '.' + className;
        }
      }
    }
  }
}
