package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
public class ProcessSnapshot {

    private static final int ownPID;

    static {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int index = runtimeName.indexOf('@');

        int parsed = -1;
        if (index > 0) {
            try {
                parsed = Integer.parseInt(runtimeName.substring(0, index));
            } catch (NumberFormatException ignored) {
            }
        }

        ownPID = parsed;
    }

    private final long heapUsageMemory;
    private final long noHeapUsageMemory;
    private final long maxHeapMemory;

    private final int currentLoadedClassCount;

    private final long totalLoadedClassCount;
    private final long unloadedClassCount;

    private final Collection<ThreadSnapshot> threads;

    private final double cpuUsage;

    private final int pid;

    public ProcessSnapshot(long heapUsageMemory, long noHeapUsageMemory, long maxHeapMemory, int currentLoadedClassCount, long totalLoadedClassCount, long unloadedClassCount, Collection<ThreadSnapshot> threads, double cpuUsage, int pid) {
        this.heapUsageMemory = heapUsageMemory;
        this.noHeapUsageMemory = noHeapUsageMemory;
        this.maxHeapMemory = maxHeapMemory;
        this.currentLoadedClassCount = currentLoadedClassCount;
        this.totalLoadedClassCount = totalLoadedClassCount;
        this.unloadedClassCount = unloadedClassCount;
        this.threads = threads;
        this.cpuUsage = cpuUsage;
        this.pid = pid;
    }

    public long getHeapUsageMemory() {
        return this.heapUsageMemory;
    }

    public long getNoHeapUsageMemory() {
        return this.noHeapUsageMemory;
    }

    public long getMaxHeapMemory() {
        return this.maxHeapMemory;
    }

    public int getCurrentLoadedClassCount() {
        return this.currentLoadedClassCount;
    }

    public long getTotalLoadedClassCount() {
        return this.totalLoadedClassCount;
    }

    public long getUnloadedClassCount() {
        return this.unloadedClassCount;
    }

    public Collection<ThreadSnapshot> getThreads() {
        return this.threads;
    }

    public double getCpuUsage() {
        return this.cpuUsage;
    }

    public int getPid() {
        return this.pid;
    }

    public static ProcessSnapshot empty() {
        return new ProcessSnapshot(-1, -1, -1, -1, -1, -1, Collections.emptyList(), -1, -1);
    }

    public static ProcessSnapshot self() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

        return new ProcessSnapshot(
                memoryMXBean.getHeapMemoryUsage().getUsed(),
                memoryMXBean.getNonHeapMemoryUsage().getUsed(),
                memoryMXBean.getHeapMemoryUsage().getMax(),
                classLoadingMXBean.getLoadedClassCount(),
                classLoadingMXBean.getTotalLoadedClassCount(),
                classLoadingMXBean.getUnloadedClassCount(),
                Thread.getAllStackTraces().keySet()
                        .stream().map(thread -> new ThreadSnapshot(thread.getId(), thread.getName(), thread.getState(), thread.isDaemon(), thread.getPriority()))
                        .collect(Collectors.toList()),
                CPUUsageResolver.getProcessCPUUsage(),
                getOwnPID()
        );
    }

    public static int getOwnPID() {
        return ProcessSnapshot.ownPID;
    }
}