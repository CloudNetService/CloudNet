package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Arrays;
import java.util.List;

public final class CommandMe extends CommandDefault {

    public CommandMe()
    {
        super("me", "cloud", "cloudnet");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties)
    {
        List<String> messages = Iterables.newArrayList();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        messages.addAll(Arrays.asList(
            " ",
            "CloudNet " + CommandMe.class.getPackage().getImplementationTitle()
                + " " + CommandMe.class.getPackage().getImplementationVersion() + " by Dytanic",
            "Discord: https://discord.gg/CPCWr7w",
            " ",
            "ClusterId: " + getCloudNet().getConfig().getClusterConfig().getClusterId(),
            "NodeId: " + getCloudNet().getConfig().getIdentity().getUniqueId(),
            "CPU usage: (P/S) " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getProcessCPUUsage()) + "/" +
                CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getSystemCPUUsage()) + "/100%",
            "Node services memory allocation: " + getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot().getUsedMemory() + "/" +
                getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot().getReservedMemory() + "/" +
                getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot().getMaxMemory() + "MB",
            "Threads: " + Thread.getAllStackTraces().keySet().size(),
            "Heap usage: " + (memoryMXBean.getHeapMemoryUsage().getUsed() / 1048576) + "/" + (memoryMXBean.getHeapMemoryUsage().getMax() / 1048576) + "MB",
            "Loaded classes: " + ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(),
            "Unloaded classes: " + ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount(),
            "Total loaded classes: " + ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount(),
            " "
        ));
        messages.add(" ");
        sender.sendMessage(messages.toArray(new String[0]));
    }
}