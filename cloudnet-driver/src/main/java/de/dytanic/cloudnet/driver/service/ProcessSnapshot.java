package de.dytanic.cloudnet.driver.service;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@ToString
@EqualsAndHashCode
public class ProcessSnapshot {

    private long heapUsageMemory, noHeapUsageMemory, maxHeapMemory;

    private int currentLoadedClassCount;

    private long totalLoadedClassCount, unloadedClassCount;

    private Collection<ThreadSnapshot> threads;

    private double cpuUsage;

    private int pid;

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

}