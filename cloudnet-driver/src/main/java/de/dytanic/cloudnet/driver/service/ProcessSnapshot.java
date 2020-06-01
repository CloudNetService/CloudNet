package de.dytanic.cloudnet.driver.service;

import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.serialization.SerializableObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@ToString
@EqualsAndHashCode
public class ProcessSnapshot implements SerializableObject {

    private long heapUsageMemory;
    private long noHeapUsageMemory;
    private long maxHeapMemory;

    private int currentLoadedClassCount;

    private long totalLoadedClassCount;
    private long unloadedClassCount;

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

    public ProcessSnapshot() {
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

    @Override
    public void write(@NotNull ProtocolBuffer buffer) {
        buffer.writeLong(this.heapUsageMemory);
        buffer.writeLong(this.noHeapUsageMemory);
        buffer.writeLong(this.maxHeapMemory);
        buffer.writeInt(this.currentLoadedClassCount);
        buffer.writeLong(this.totalLoadedClassCount);
        buffer.writeLong(this.unloadedClassCount);
        buffer.writeObjectCollection(this.threads);
        buffer.writeDouble(this.cpuUsage);
        buffer.writeInt(this.pid);
    }

    @Override
    public void read(@NotNull ProtocolBuffer buffer) {
        this.heapUsageMemory = buffer.readLong();
        this.noHeapUsageMemory = buffer.readLong();
        this.maxHeapMemory = buffer.readLong();
        this.currentLoadedClassCount = buffer.readInt();
        this.totalLoadedClassCount = buffer.readLong();
        this.unloadedClassCount = buffer.readLong();
        this.threads = buffer.readObjectCollection(ThreadSnapshot.class);
        this.cpuUsage = buffer.readDouble();
        this.pid = buffer.readInt();
    }
}