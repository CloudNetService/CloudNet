package de.dytanic.cloudnet.ext.report.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.TypeAdapters;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.command.CommandInfo;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.document.gson.JsonDocumentTypeAdapter;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.IModuleTaskEntry;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CommandReport extends Command {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(TypeAdapters.newFactory(JsonDocument.class, new JsonDocumentTypeAdapter()))
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private final DateFormat
            dateFormat = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss"),
            logFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");

    public CommandReport() {
        super("report", "reports");

        this.usage = "report";
        this.permission = "cloudnet.console.command.report";
        this.prefix = "cloudnet-report";
        this.description = LanguageManager.getMessage("module-report-command-report-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        File directory = new File(CloudNetReportModule.getInstance().getModuleWrapper().getDataFolder(), "reports");
        directory.mkdirs();

        long millis = System.currentTimeMillis();
        File file = new File(directory, dateFormat.format(millis) + ".report");

        if (file.exists()) {
            return;
        }

        try (FileWriter fileWriter = new FileWriter(file, false);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, true)) {
            postOutput(printWriter, millis);

            String postData = new String(byteArrayOutputStream.toByteArray());

            fileWriter.write(postData);
            fileWriter.flush();

            sender.sendMessage(LanguageManager.getMessage("module-report-command-report-post-success")
                    .replace("%file%", file.getAbsolutePath())
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void postOutput(Writer w, long millis) throws IOException {
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
                                    "[" + logFormat.format(logEntry.getTimeStamp()) + "] " +
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
                writer.println("Thread " + entry.getKey().getId() + " | " + entry.getKey().getName() + " | " + entry.getKey().getState());
                writer.println("- Daemon: " + entry.getKey().isDaemon() + " | isAlive: " + entry.getKey().isAlive() + " | Priority: " + entry.getKey().getPriority());
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
            for (IClusterNodeServer clusterNodeServer : CloudNet.getInstance().getClusterNodeServerProvider().getNodeServers()) {
                writer.println("Node: " + clusterNodeServer.getNodeInfo().getUniqueId() + " | Connected: " + clusterNodeServer.isConnected());
                gson.toJson(clusterNodeServer.getNodeInfo(), writer);

                if (clusterNodeServer.getNodeInfoSnapshot() != null) {
                    writer.println();
                    gson.toJson(clusterNodeServer.getNodeInfoSnapshot(), writer);
                }

                writer.println();
            }

            writer.println("###################################################################################");
            writer.println("Services: " + CloudNetDriver.getInstance().getCloudServices().size());
            for (ServiceInfoSnapshot serviceInfoSnapshot : CloudNetDriver.getInstance().getCloudServices()) {
                writer.println("* Service " + serviceInfoSnapshot.getServiceId().getName() + " | " + serviceInfoSnapshot.getServiceId().getUniqueId());
                gson.toJson(serviceInfoSnapshot, writer);
                writer.println();

                writer.println("Console receivedMessages:");
                for (String entry : CloudNetDriver.getInstance().getCachedLogMessagesFromService(serviceInfoSnapshot.getServiceId().getUniqueId())) {
                    writer.println(entry);
                }

                writer.println();
            }

            writer.println("###################################################################################");
            writer.println("Commands:");
            for (CommandInfo commandInfo : CloudNet.getInstance().getCommandMap().getCommandInfos()) {
                gson.toJson(commandInfo, writer);
                writer.println();
            }

            writer.println("###################################################################################");
            writer.println("Modules:");
            for (IModuleWrapper moduleWrapper : CloudNetDriver.getInstance().getModuleProvider().getModules()) {
                writer.println(moduleWrapper.getModuleConfiguration().getName() + " | " + moduleWrapper.getModuleLifeCycle());
                writer.println();
                gson.toJson(moduleWrapper.getModuleConfigurationSource(), writer);
                writer.println();
                writer.println("- ModuleTasks");

                for (Map.Entry<ModuleLifeCycle, List<IModuleTaskEntry>> moduleLifeCycleListEntry : moduleWrapper.getModuleTasks().entrySet()) {
                    writer.println("ModuleTask: " + moduleLifeCycleListEntry.getKey());

                    for (IModuleTaskEntry moduleTaskEntry : moduleLifeCycleListEntry.getValue()) {
                        writer.println("Order: " + moduleTaskEntry.getTaskInfo().order() + " | " + moduleTaskEntry.getHandler().getName());
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
}