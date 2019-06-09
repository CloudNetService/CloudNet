package de.dytanic.cloudnet.driver.service;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

@ToString
@EqualsAndHashCode
public final class ProcessConfiguration {

    protected ServiceEnvironmentType environment;

    protected int maxHeapMemorySize;

    protected Collection<String> jvmOptions;

    public ProcessConfiguration(ServiceEnvironmentType environment, int maxHeapMemorySize, Collection<String> jvmOptions) {
        this.environment = environment;
        this.maxHeapMemorySize = maxHeapMemorySize;
        this.jvmOptions = jvmOptions;
    }

    public ServiceEnvironmentType getEnvironment() {
        return this.environment;
    }

    public void setEnvironment(ServiceEnvironmentType environment) {
        this.environment = environment;
    }

    public int getMaxHeapMemorySize() {
        return this.maxHeapMemorySize;
    }

    public void setMaxHeapMemorySize(int maxHeapMemorySize) {
        this.maxHeapMemorySize = maxHeapMemorySize;
    }

    public Collection<String> getJvmOptions() {
        return this.jvmOptions;
    }

    public void setJvmOptions(Collection<String> jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

}