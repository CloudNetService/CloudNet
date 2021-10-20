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

package eu.cloudnetservice.cloudnet.ext.report.command;

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import com.sun.management.HotSpotDiagnosticMXBean;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.ext.report.CloudNetReportModule;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.management.MBeanServer;

@Description("Create heap and thread dumps of the cloud for debugging purposes")
@CommandPermission("cloudnet.command.report")
public final class CommandReport {

  private static final Logger LOGGER = LogManager.getLogger(CommandReport.class);

  private final CloudNetReportModule reportModule;

  public CommandReport(CloudNetReportModule reportModule) {
    this.reportModule = reportModule;
  }

  @CommandMethod("report thread-dump")
  public void reportThreadDump(CommandSource source) {
    Path file = this.reportModule.getCurrentRecordDirectory().resolve(System.currentTimeMillis() + "-threaddump.txt");

    if (this.createThreadDump(file)) {
      source.sendMessage(
        LanguageManager.getMessage("module-report-thread-dump-success").replace("%file%", file.toString()));
    } else {
      source.sendMessage(LanguageManager.getMessage("module-report-thread-dump-failed"));
    }
  }

  @CommandMethod("report heap-dump")
  public void reportHeapDump(CommandSource source, @Flag("live") boolean live) {
    Path file = this.reportModule.getCurrentRecordDirectory().resolve(System.currentTimeMillis() + "-heapdump.hprof");

    if (this.createHeapDump(file, live)) {
      source.sendMessage(
        LanguageManager.getMessage("module-report-heap-dump-success").replace("%file%", file.toString()));
    } else {
      source.sendMessage(LanguageManager.getMessage("module-report-heap-dump-failed"));
    }
  }

  private boolean createThreadDump(Path path) {
    StringBuilder builder = new StringBuilder();
    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    for (ThreadInfo threadInfo : threadBean.dumpAllThreads(threadBean.isObjectMonitorUsageSupported(),
      threadBean.isSynchronizerUsageSupported())) {
      builder.append(threadInfo.toString());
    }

    try {
      Files.write(path, builder.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW,
        StandardOpenOption.WRITE);
      return true;
    } catch (IOException exception) {
      LOGGER.severe("Unable to create thread dump", exception);
      return false;
    }
  }

  private boolean createHeapDump(Path path, boolean live) {
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
        server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
      mxBean.dumpHeap(path.toString(), live);
      return true;
    } catch (IOException exception) {
      LOGGER.severe("Unable to create heap dump", exception);
      return false;
    }
  }
}
