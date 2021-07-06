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

package de.dytanic.cloudnet.ext.report.command;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;
import com.sun.management.HotSpotDiagnosticMXBean;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.gson.JsonDocumentTypeAdapter;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.command.CommandInfo;
import de.dytanic.cloudnet.driver.module.IModuleTaskEntry;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;

public final class CommandReport extends SubCommandHandler {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
  private static final DateFormat LOG_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");

  private final Gson gson = new GsonBuilder()
    .registerTypeAdapterFactory(TypeAdapters.newFactory(JsonDocument.class, new JsonDocumentTypeAdapter()))
    .setPrettyPrinting()
    .serializeNulls()
    .create();

  public CommandReport() {
    super("report", "reports");

    this.usage = "report";
    this.permission = "cloudnet.command.report";
    this.prefix = "cloudnet-report";
    this.description = LanguageManager.getMessage("module-report-command-report-description");

    super.setSubCommands(SubCommandBuilder.create()
      .preExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
        Path directory = CloudNetReportModule.getInstance().getModuleWrapper().getDataDirectory().resolve("reports");
        FileUtils.createDirectoryReported(directory);

        internalProperties.put("dir", directory);
      })
      .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
        String filePath = (String) internalProperties.get("filePath");

        if (filePath != null) {
          sender.sendMessage(LanguageManager.getMessage("module-report-command-report-post-success")
            .replace("%file%", filePath));
        }
      })
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          long millis = System.currentTimeMillis();
          Path file = ((Path) internalProperties.get("dir")).resolve(DATE_FORMAT.format(millis) + ".report");

          if (Files.exists(file)) {
            return;
          }

          internalProperties.put("filePath", file.toAbsolutePath().toString());
          try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file),
            StandardCharsets.UTF_8)) {
            this.postReportOutput(writer, millis);
          } catch (IOException exception) {
            exception.printStackTrace();
          }
        },
        exactStringIgnoreCase("cloud"))
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          long millis = System.currentTimeMillis();
          Path file = ((Path) internalProperties.get("dir")).resolve(DATE_FORMAT.format(millis) + "-heapdump.hprof");

          if (Files.exists(file)) {
            return;
          }

          String filePath = file.toAbsolutePath().toString();
          internalProperties.put("filePath", filePath);
          this.createHeapDump(filePath);
        },
        exactStringIgnoreCase("heap"))
      .getSubCommands());
  }

  private void postReportOutput(Writer w, long millis) throws IOException {
    try (PrintWriter writer = new PrintWriter(w, true)) {
      writer.println("Report from " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(millis));
      writer.println();
      writer.println("Last Event Publisher Class: " + CloudNetReportModule.getInstance().getEventClass());
      writer.println();

      writer.println("Last log lines");
      for (LogEntry logEntry : CloudNet.getInstance().getQueuedConsoleLogHandler().getCachedQueuedLogEntries()) {
        if (logEntry.getMessages() != null) {
          for (String message : logEntry.getMessages()) {
            if (message != null) {
              writer.println(
                "[" + LOG_FORMAT.format(logEntry.getTimeStamp()) + "] " +
                  logEntry.getLogLevel().getUpperName() + " | Thread: " +
                  logEntry.getThread().getName() + " | Class: " +
                  logEntry.getClazz().getName() + ": " +
                  message
              );

              if (logEntry.getThrowable() != null) {
                logEntry.getThrowable().printStackTrace(writer);
              }
            }
          }
        }
      }

      writer.println();
      writer.println("###################################################################################");
      System.getProperties().store(writer, "System Properties");
      writer.println();

      Collection<Map.Entry<Thread, StackTraceElement[]>> threads = Thread.getAllStackTraces().entrySet();

      writer.println("Threads: " + threads.size());
      for (Map.Entry<Thread, StackTraceElement[]> entry : threads) {
        writer.println(
          "Thread " + entry.getKey().getId() + " | " + entry.getKey().getName() + " | " + entry.getKey().getState());
        writer.println(
          "- Daemon: " + entry.getKey().isDaemon() + " | isAlive: " + entry.getKey().isAlive() + " | Priority: " + entry
            .getKey().getPriority());
        writer.println("- Context ClassLoader: " + (entry.getKey().getContextClassLoader() != null ?
          entry.getKey().getContextClassLoader().getClass().getName() : "not defined"));
        writer.println("- ThreadGroup: " + entry.getKey().getThreadGroup().getName());
        writer.println();

        writer.println("- Stack");
        writer.println();

        for (StackTraceElement element : entry.getValue()) {
          writer.println(element.toString());
        }

        writer.println();
      }

      writer.println("###################################################################################");
      writer.println("Remote nodes: ");
      for (IClusterNodeServer clusterNodeServer : CloudNet.getInstance().getClusterNodeServerProvider()
        .getNodeServers()) {
        writer.println("Node: " + clusterNodeServer.getNodeInfo().getUniqueId() + " | Connected: " + clusterNodeServer
          .isConnected());
        this.gson.toJson(clusterNodeServer.getNodeInfo(), writer);

        if (clusterNodeServer.getNodeInfoSnapshot() != null) {
          writer.println();
          this.gson.toJson(clusterNodeServer.getNodeInfoSnapshot(), writer);
        }

        writer.println();
      }

      writer.println("###################################################################################");
      writer.println("Services: " + CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices().size());
      for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServiceProvider()
        .getCloudServices()) {
        writer.println(
          "* Service " + serviceInfoSnapshot.getServiceId().getName() + " | " + serviceInfoSnapshot.getServiceId()
            .getUniqueId());
        this.gson.toJson(serviceInfoSnapshot, writer);
        writer.println();

        writer.println("Console receivedMessages:");
        for (String entry : serviceInfoSnapshot.provider().getCachedLogMessages()) {
          writer.println(entry);
        }

        writer.println();
      }

      writer.println("###################################################################################");
      writer.println("Commands:");
      for (CommandInfo commandInfo : CloudNet.getInstance().getCommandMap().getCommandInfos()) {
        this.gson.toJson(commandInfo, writer);
        writer.println();
      }

      writer.println("###################################################################################");
      writer.println("Modules:");
      for (IModuleWrapper moduleWrapper : CloudNetDriver.getInstance().getModuleProvider().getModules()) {
        writer.println(moduleWrapper.getModuleConfiguration().getName() + " | " + moduleWrapper.getModuleLifeCycle());
        writer.println();
        this.gson.toJson(moduleWrapper.getModuleConfigurationSource(), writer);
        writer.println();
        writer.println("- ModuleTasks");

        for (Map.Entry<ModuleLifeCycle, List<IModuleTaskEntry>> moduleLifeCycleListEntry : moduleWrapper
          .getModuleTasks().entrySet()) {
          writer.println("ModuleTask: " + moduleLifeCycleListEntry.getKey());

          for (IModuleTaskEntry moduleTaskEntry : moduleLifeCycleListEntry.getValue()) {
            writer.println(
              "Order: " + moduleTaskEntry.getTaskInfo().order() + " | " + moduleTaskEntry.getHandler().getName());
          }
        }

        writer.println();
      }

      writer.println("###################################################################################");
      writer.println("Service Registry:");
      for (Class<?> c : CloudNetDriver.getInstance().getServicesRegistry().getProvidedServices()) {
        writer.println("Registry Item Class: " + c.getName());

        for (Object item : CloudNetDriver.getInstance().getServicesRegistry().getServices(c)) {
          writer.println("- " + item.getClass().getName());
        }

        writer.println();
      }
    }
  }

  private void createHeapDump(String filePath) {
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
        server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
      mxBean.dumpHeap(filePath, false);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

}
