package de.dytanic.cloudnet.driver.service;

import java.util.Collection;

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

    public int getMaxHeapMemorySize() {
        return this.maxHeapMemorySize;
    }

    public Collection<String> getJvmOptions() {
        return this.jvmOptions;
    }

    public void setEnvironment(ServiceEnvironmentType environment) {
        this.environment = environment;
    }

    public void setMaxHeapMemorySize(int maxHeapMemorySize) {
        this.maxHeapMemorySize = maxHeapMemorySize;
    }

    public void setJvmOptions(Collection<String> jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ProcessConfiguration)) return false;
        final ProcessConfiguration other = (ProcessConfiguration) o;
        final Object this$environment = this.getEnvironment();
        final Object other$environment = other.getEnvironment();
        if (this$environment == null ? other$environment != null : !this$environment.equals(other$environment))
            return false;
        if (this.getMaxHeapMemorySize() != other.getMaxHeapMemorySize()) return false;
        final Object this$jvmOptions = this.getJvmOptions();
        final Object other$jvmOptions = other.getJvmOptions();
        if (this$jvmOptions == null ? other$jvmOptions != null : !this$jvmOptions.equals(other$jvmOptions))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $environment = this.getEnvironment();
        result = result * PRIME + ($environment == null ? 43 : $environment.hashCode());
        result = result * PRIME + this.getMaxHeapMemorySize();
        final Object $jvmOptions = this.getJvmOptions();
        result = result * PRIME + ($jvmOptions == null ? 43 : $jvmOptions.hashCode());
        return result;
    }

    public String toString() {
        return "ProcessConfiguration(environment=" + this.getEnvironment() + ", maxHeapMemorySize=" + this.getMaxHeapMemorySize() + ", jvmOptions=" + this.getJvmOptions() + ")";
    }
}