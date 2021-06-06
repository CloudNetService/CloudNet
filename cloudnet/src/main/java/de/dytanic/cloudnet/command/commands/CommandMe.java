package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CommandMe extends CommandDefault {

  public CommandMe() {
    super("me", "cloud", "cloudnet");
  }

  @Override
  public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    List<String> messages = new ArrayList<>(Arrays.asList(
      " ",
      "CloudNet " + CommandMe.class.getPackage().getImplementationTitle()
        + " " + CommandMe.class.getPackage().getImplementationVersion() + " by Dytanic & the CloudNet Community",
      "Discord: https://discord.cloudnetservice.eu/",
      " ",
      "ClusterId: " + this.getCloudNet().getConfig().getClusterConfig().getClusterId(),
      "NodeId: " + this.getCloudNet().getConfig().getIdentity().getUniqueId(),
      "Head-NodeId: " + this.getCloudNet().getClusterNodeServerProvider().getHeadNode().getNodeInfo().getUniqueId(),
      "CPU usage: (P/S) " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getProcessCPUUsage()) + "/"
        +
        CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT.format(CPUUsageResolver.getSystemCPUUsage()) + "/100%",
      "Node services memory allocation (U/R/M): " + this.getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot()
        .getUsedMemory() + "/" +
        this.getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot().getReservedMemory() + "/" +
        this.getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot().getMaxMemory() + " MB",
      "Threads: " + Thread.getAllStackTraces().keySet().size(),
      "Heap usage: " + (memoryMXBean.getHeapMemoryUsage().getUsed() / 1048576) + "/" + (
        memoryMXBean.getHeapMemoryUsage().getMax() / 1048576) + "MB",
      "Loaded classes: " + ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(),
      "Unloaded classes: " + ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount(),
      "Total loaded classes: " + ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount(),
      " "
    ));
    sender.sendMessage(messages.toArray(new String[0]));
  }
}
