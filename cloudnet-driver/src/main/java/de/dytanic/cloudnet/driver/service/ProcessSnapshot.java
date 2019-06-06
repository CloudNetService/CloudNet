package de.dytanic.cloudnet.driver.service;

import java.util.Collection;

public class ProcessSnapshot {

    private long heapUsageMemory, noHeapUsageMemory, maxHeapMemory;

    private int currentLoadedClassCount;

    private long totalLoadedClassCount, unloadedClassCount;

    private Collection<ThreadSnapshot> threads;

    private double cpuUsage;

    public ProcessSnapshot(long heapUsageMemory, long noHeapUsageMemory, long maxHeapMemory, int currentLoadedClassCount, long totalLoadedClassCount, long unloadedClassCount, Collection<ThreadSnapshot> threads, double cpuUsage) {
        this.heapUsageMemory = heapUsageMemory;
        this.noHeapUsageMemory = noHeapUsageMemory;
        this.maxHeapMemory = maxHeapMemory;
        this.currentLoadedClassCount = currentLoadedClassCount;
        this.totalLoadedClassCount = totalLoadedClassCount;
        this.unloadedClassCount = unloadedClassCount;
        this.threads = threads;
        this.cpuUsage = cpuUsage;
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ProcessSnapshot)) return false;
        final ProcessSnapshot other = (ProcessSnapshot) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getHeapUsageMemory() != other.getHeapUsageMemory()) return false;
        if (this.getNoHeapUsageMemory() != other.getNoHeapUsageMemory()) return false;
        if (this.getMaxHeapMemory() != other.getMaxHeapMemory()) return false;
        if (this.getCurrentLoadedClassCount() != other.getCurrentLoadedClassCount()) return false;
        if (this.getTotalLoadedClassCount() != other.getTotalLoadedClassCount()) return false;
        if (this.getUnloadedClassCount() != other.getUnloadedClassCount()) return false;
        final Object this$threads = this.getThreads();
        final Object other$threads = other.getThreads();
        if (this$threads == null ? other$threads != null : !this$threads.equals(other$threads)) return false;
        if (Double.compare(this.getCpuUsage(), other.getCpuUsage()) != 0) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ProcessSnapshot;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $heapUsageMemory = this.getHeapUsageMemory();
        result = result * PRIME + (int) ($heapUsageMemory >>> 32 ^ $heapUsageMemory);
        final long $noHeapUsageMemory = this.getNoHeapUsageMemory();
        result = result * PRIME + (int) ($noHeapUsageMemory >>> 32 ^ $noHeapUsageMemory);
        final long $maxHeapMemory = this.getMaxHeapMemory();
        result = result * PRIME + (int) ($maxHeapMemory >>> 32 ^ $maxHeapMemory);
        result = result * PRIME + this.getCurrentLoadedClassCount();
        final long $totalLoadedClassCount = this.getTotalLoadedClassCount();
        result = result * PRIME + (int) ($totalLoadedClassCount >>> 32 ^ $totalLoadedClassCount);
        final long $unloadedClassCount = this.getUnloadedClassCount();
        result = result * PRIME + (int) ($unloadedClassCount >>> 32 ^ $unloadedClassCount);
        final Object $threads = this.getThreads();
        result = result * PRIME + ($threads == null ? 43 : $threads.hashCode());
        final long $cpuUsage = Double.doubleToLongBits(this.getCpuUsage());
        result = result * PRIME + (int) ($cpuUsage >>> 32 ^ $cpuUsage);
        return result;
    }

    public String toString() {
        return "ProcessSnapshot(heapUsageMemory=" + this.getHeapUsageMemory() + ", noHeapUsageMemory=" + this.getNoHeapUsageMemory() + ", maxHeapMemory=" + this.getMaxHeapMemory() + ", currentLoadedClassCount=" + this.getCurrentLoadedClassCount() + ", totalLoadedClassCount=" + this.getTotalLoadedClassCount() + ", unloadedClassCount=" + this.getUnloadedClassCount() + ", threads=" + this.getThreads() + ", cpuUsage=" + this.getCpuUsage() + ")";
    }
}