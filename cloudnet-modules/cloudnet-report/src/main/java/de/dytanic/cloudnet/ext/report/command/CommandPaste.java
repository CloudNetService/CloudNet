package de.dytanic.cloudnet.ext.report.command;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.Command;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.DefaultFileLogHandler;
import de.dytanic.cloudnet.common.logging.DefaultLogFormatter;
import de.dytanic.cloudnet.common.logging.IFormatter;
import de.dytanic.cloudnet.common.logging.ILogHandler;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.*;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CommandPaste extends Command {

    private final IFormatter logFormatter = new DefaultLogFormatter();

    public CommandPaste() {
        super("paste", "haste");

        this.usage = "paste service|node|modules|tasks|all";
        this.permission = "cloudnet.console.command.paste";
        this.prefix = "cloudnet-report";
        this.description = LanguageManager.getMessage("module-report-command-paste-description");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if (args.length == 0) {
            sender.sendMessage(
                    "paste service <name : uniqueId>",
                    "paste node | maxLines=the max lines of node log that should be pasted",
                    "paste modules",
                    "paste tasks",
                    "paste all"
            );
            return;
        }

        String content = null;

        if (args[0].equalsIgnoreCase("service") && args.length == 2) {
            ServiceInfoSnapshot serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceByNameOrUniqueId(args[1]);

            if (serviceInfoSnapshot != null) {
                content = this.pasteService(serviceInfoSnapshot);
            }
        } else if (args[0].equalsIgnoreCase("node")) {
            content = this.pasteNode(properties);
        } else if (args[0].equalsIgnoreCase("modules")) {
            content = this.pasteModules();
        } else if (args[0].equalsIgnoreCase("tasks")) {
            content = this.pasteTasks();
        } else if (args[0].equalsIgnoreCase("all")) {
            StringBuilder builder = new StringBuilder();
            builder.append("Total running services: ").append(CloudNet.getInstance().getCloudServiceProvider().getCloudServices().size()).append('\n');
            this.appendComponent(builder, "Node", this.pasteNode(properties), false);
            this.appendComponent(builder, "Modules", this.pasteModules(), false);
            this.appendComponent(builder, "Tasks", this.pasteTasks(), true);
            content = builder.toString();
        } else {
            return;
        }

        String url = CloudNetReportModule.getInstance().executePaste(content);
        if (url != null) {
            sender.sendMessage(LanguageManager.getMessage("module-report-command-paste-success").replace("%url%", url));
        } else {
            sender.sendMessage(LanguageManager.getMessage("module-report-command-paste-failed").replace("%url%", CloudNetReportModule.getInstance().getPasteURL()));
        }
    }

    private void appendComponent(StringBuilder builder, String component, String content, boolean lastComponent) {
        builder.append(component).append(": \n");
        for (String line : content.split("\n")) {
            builder.append("  ").append(line).append('\n');
        }
        if (!lastComponent) {
            builder.append("\n\n\n");
        }
    }

    private String pasteTasks() {
        StringBuilder builder = new StringBuilder();

        for (ServiceTask serviceTask : CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()) {
            builder.append(serviceTask.getName()).append(":\n");

            builder.append("  Minimal Services: ").append(serviceTask.getMinServiceCount()).append('\n');
            builder.append("  Associated nodes: ").append(serviceTask.getAssociatedNodes().toString()).append('\n');
            builder.append("  Groups: ").append(serviceTask.getGroups().toString()).append('\n');
            builder.append("  Start Port: ").append(serviceTask.getStartPort()).append('\n');
            builder.append("  Static services: ").append(serviceTask.isStaticServices()).append('\n');
            builder.append("  Auto delete on stop: ").append(serviceTask.isAutoDeleteOnStop()).append('\n');
            builder.append("  Maintenance: ").append(serviceTask.isMaintenance()).append('\n');
            builder.append("  Delete files on stop: ").append(serviceTask.getDeletedFilesAfterStop()).append('\n');
            builder.append("  Environment: ").append(serviceTask.getProcessConfiguration().getEnvironment()).append('\n');
            builder.append("  Max HeapMemory: ").append(serviceTask.getProcessConfiguration().getMaxHeapMemorySize()).append('\n');
            builder.append("  JVM Options: ").append(serviceTask.getProcessConfiguration().getJvmOptions().toString()).append('\n');
            builder.append("  Json: \n");
            builder.append(JsonDocument.newDocument(serviceTask).toPrettyJson()).append('\n');
        }

        return builder.length() > 0 ? builder.substring(0, builder.length() - 1) : builder.toString();
    }

    private String pasteService(ServiceInfoSnapshot serviceInfoSnapshot) {
        StringBuilder builder = new StringBuilder();

        for (String line : CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).getCachedLogMessages()) {
            builder.append(line).append('\n');
        }

        builder.append("\nServiceInfoSnapshot\n");

        builder.append(new JsonDocument(serviceInfoSnapshot).toPrettyJson());

        return builder.toString();
    }

    private String pasteNode(Properties properties) {
        StringBuilder builder = new StringBuilder();

        builder.append("CloudNet-Version: ")
                .append(CloudNet.class.getPackage().getImplementationVersion())
                .append(" - ")
                .append(CloudNet.class.getPackage().getImplementationTitle())
                .append('\n');

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        builder.append("ClusterId: ").append(CloudNet.getInstance().getConfig().getClusterConfig().getClusterId()).append('\n');
        builder.append("NodeId: ").append(CloudNet.getInstance().getConfig().getIdentity().getUniqueId()).append('\n');
        builder.append("CPU usage: (P/S) ")
                .append(CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getProcessCPUUsage()))
                .append("/")
                .append(CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getSystemCPUUsage()))
                .append("/100%").append('\n');
        builder.append("Node services memory allocation: ")
                .append(CloudNet.getInstance().getCurrentNetworkClusterNodeInfoSnapshot().getUsedMemory())
                .append("/")
                .append(CloudNet.getInstance().getCurrentNetworkClusterNodeInfoSnapshot().getReservedMemory())
                .append("/")
                .append(CloudNet.getInstance().getCurrentNetworkClusterNodeInfoSnapshot().getMaxMemory())
                .append("MB")
                .append('\n');
        builder.append("Threads: ").append(Thread.getAllStackTraces().keySet().size()).append('\n');
        builder.append("Heap usage: ")
                .append(memoryMXBean.getHeapMemoryUsage().getUsed() / 1048576)
                .append("/")
                .append(memoryMXBean.getHeapMemoryUsage().getMax() / 1048576)
                .append("MB")
                .append('\n');

        builder.append('\n');

        File logFile = null;
        for (ILogHandler logHandler : CloudNet.getInstance().getLogger().getLogHandlers()) {
            if (logHandler instanceof DefaultFileLogHandler) {
                logFile = ((DefaultFileLogHandler) logHandler).getEntry();
            }
        }

        try {
            int maxLines = properties.containsKey("maxLines") && Validate.testStringParseToInt(properties.get("maxLines")) ?
                    Integer.parseInt(properties.get("maxLines")) : -1;
            if (maxLines <= 0) {
                maxLines = 512;
            }

            List<String> logLines;

            if (logFile != null) {
                List<String> lines = Files.readAllLines(logFile.toPath());
                if (lines.size() >= maxLines) {
                    logLines = lines.stream().skip(lines.size() - maxLines).collect(Collectors.toList());
                } else {
                    logLines = lines;
                }
            } else {
                logLines = CloudNet.getInstance().getQueuedConsoleLogHandler().getCachedQueuedLogEntries().stream()
                        .map(this.logFormatter::format)
                        .collect(Collectors.toList());
            }

            for (String logLine : logLines) {
                builder.append(logLine).append('\n');
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        builder.append("\nConfig:\n");
        builder.append(JsonDocument.newDocument(CloudNet.getInstance().getConfig()).toPrettyJson());

        return builder.toString();
    }

    private String pasteModules() {
        IModuleProvider moduleProvider = CloudNet.getInstance().getModuleProvider();

        StringBuilder builder = new StringBuilder();

        builder.append("Installed modules:\n");
        builder.append(moduleProvider.getModules().stream().map(moduleWrapper -> moduleWrapper.getModuleConfiguration().getName()).collect(Collectors.joining(", ")));
        builder.append("\n\n");

        for (IModuleWrapper moduleWrapper : moduleProvider.getModules()) {
            ModuleConfiguration configuration = moduleWrapper.getModuleConfiguration();

            Map<String, String> repositoryURLs = new HashMap<>();
            if (configuration.getRepos() != null) {
                for (ModuleRepository repository : configuration.getRepos()) {
                    if (repository.getName() != null && repository.getUrl() != null) {
                        repositoryURLs.put(repository.getName(), repository.getUrl());
                    }
                }
            }
            repositoryURLs.putAll(moduleWrapper.getDefaultRepositories());


            builder.append(configuration.getGroup()).append(":").append(configuration.getName()).append(":").append(configuration.getVersion()).append(":\n");
            builder.append("  Description: ").append(configuration.getDescription()).append('\n');
            builder.append("  Website: ").append(configuration.getWebsite()).append('\n');
            builder.append("  Author: ").append(configuration.getAuthor()).append('\n');
            builder.append("  Version: ").append(configuration.getVersion()).append('\n');
            builder.append("  Main: ").append(configuration.getMain()).append('\n');
            builder.append("  Restart on CloudNet reload: ").append(!configuration.isRuntimeModule()).append('\n');

            if (configuration.getDependencies() != null) {
                builder.append("  Dependencies: \n");
                for (ModuleDependency dependency : configuration.getDependencies()) {
                    if (dependency == null || dependency.getGroup() == null || dependency.getName() == null || dependency.getVersion() == null) {
                        continue;
                    }

                    if (dependency.getUrl() == null && dependency.getRepo() == null) {

                        builder.append("    Cloud Module - ").append(dependency.getGroup()).append(":").append(dependency.getName()).append(":").append(dependency.getVersion()).append('\n');

                    } else {
                        String repositoryURL = repositoryURLs.get(dependency.getRepo());
                        if (repositoryURL != null) {
                            builder.append("    Maven - ").append(dependency.getGroup()).append(":").append(dependency.getName()).append(":").append(dependency.getVersion()).append('\n');
                        }
                    }
                }
            }

            if (configuration.getRepos() != null) {
                builder.append("  Repositories: \n");
                for (ModuleRepository repository : configuration.getRepos()) {
                    builder.append("    ").append(repository.getName()).append(": ").append(repository.getUrl()).append('\n');
                }
            }

            if (configuration.getProperties() != null) {
                builder.append("  Properties: \n");
                builder.append(configuration.getProperties().toPrettyJson()).append('\n');
            }

            if (moduleWrapper.getModule() instanceof DriverModule) {
                JsonDocument config = ((DriverModule) moduleWrapper.getModule()).getConfig();
                if (config != null) {
                    builder.append("  Config:");
                    if (configuration.storesSensitiveData()) {
                        builder.append(" Hidden because of sensitive data\n");
                    } else {
                        builder.append('\n').append(config.toPrettyJson()).append('\n');
                    }
                }
            }
        }

        return builder.toString();
    }

}