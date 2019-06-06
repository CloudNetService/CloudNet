package de.dytanic.cloudnet.ext.smart.util;

import de.dytanic.cloudnet.ext.smart.template.TemplateInstaller;

public class SmartServiceTaskConfig implements Comparable<SmartServiceTaskConfig> {

    protected String task;

    protected int priority;

    protected boolean directTemplatesAndInclusionsSetup;

    protected int preparedServices;

    protected int minServiceOnlineCount;

    protected boolean dynamicMemoryAllocation;

    protected int dynamicMemoryAllocationRange;

    protected int percentOfPlayersToCheckShouldAutoStopTheServiceInFuture;

    protected int autoStopTimeByUnusedServiceInSeconds;

    protected boolean switchToPreparedServiceAfterAutoStopTimeByUnusedService;

    protected int percentOfPlayersForANewServiceByInstance;

    protected int forAnewInstanceDelayTimeInSeconds;

    protected TemplateInstaller templateInstaller;

    public SmartServiceTaskConfig(String task, int priority, boolean directTemplatesAndInclusionsSetup, int preparedServices, int minServiceOnlineCount, boolean dynamicMemoryAllocation, int dynamicMemoryAllocationRange, int percentOfPlayersToCheckShouldAutoStopTheServiceInFuture, int autoStopTimeByUnusedServiceInSeconds, boolean switchToPreparedServiceAfterAutoStopTimeByUnusedService, int percentOfPlayersForANewServiceByInstance, int forAnewInstanceDelayTimeInSeconds, TemplateInstaller templateInstaller) {
        this.task = task;
        this.priority = priority;
        this.directTemplatesAndInclusionsSetup = directTemplatesAndInclusionsSetup;
        this.preparedServices = preparedServices;
        this.minServiceOnlineCount = minServiceOnlineCount;
        this.dynamicMemoryAllocation = dynamicMemoryAllocation;
        this.dynamicMemoryAllocationRange = dynamicMemoryAllocationRange;
        this.percentOfPlayersToCheckShouldAutoStopTheServiceInFuture = percentOfPlayersToCheckShouldAutoStopTheServiceInFuture;
        this.autoStopTimeByUnusedServiceInSeconds = autoStopTimeByUnusedServiceInSeconds;
        this.switchToPreparedServiceAfterAutoStopTimeByUnusedService = switchToPreparedServiceAfterAutoStopTimeByUnusedService;
        this.percentOfPlayersForANewServiceByInstance = percentOfPlayersForANewServiceByInstance;
        this.forAnewInstanceDelayTimeInSeconds = forAnewInstanceDelayTimeInSeconds;
        this.templateInstaller = templateInstaller;
    }

    @Override
    public int compareTo(SmartServiceTaskConfig o) {
        return priority + o.priority;
    }

    public String getTask() {
        return this.task;
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean isDirectTemplatesAndInclusionsSetup() {
        return this.directTemplatesAndInclusionsSetup;
    }

    public int getPreparedServices() {
        return this.preparedServices;
    }

    public int getMinServiceOnlineCount() {
        return this.minServiceOnlineCount;
    }

    public boolean isDynamicMemoryAllocation() {
        return this.dynamicMemoryAllocation;
    }

    public int getDynamicMemoryAllocationRange() {
        return this.dynamicMemoryAllocationRange;
    }

    public int getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture() {
        return this.percentOfPlayersToCheckShouldAutoStopTheServiceInFuture;
    }

    public int getAutoStopTimeByUnusedServiceInSeconds() {
        return this.autoStopTimeByUnusedServiceInSeconds;
    }

    public boolean isSwitchToPreparedServiceAfterAutoStopTimeByUnusedService() {
        return this.switchToPreparedServiceAfterAutoStopTimeByUnusedService;
    }

    public int getPercentOfPlayersForANewServiceByInstance() {
        return this.percentOfPlayersForANewServiceByInstance;
    }

    public int getForAnewInstanceDelayTimeInSeconds() {
        return this.forAnewInstanceDelayTimeInSeconds;
    }

    public TemplateInstaller getTemplateInstaller() {
        return this.templateInstaller;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDirectTemplatesAndInclusionsSetup(boolean directTemplatesAndInclusionsSetup) {
        this.directTemplatesAndInclusionsSetup = directTemplatesAndInclusionsSetup;
    }

    public void setPreparedServices(int preparedServices) {
        this.preparedServices = preparedServices;
    }

    public void setMinServiceOnlineCount(int minServiceOnlineCount) {
        this.minServiceOnlineCount = minServiceOnlineCount;
    }

    public void setDynamicMemoryAllocation(boolean dynamicMemoryAllocation) {
        this.dynamicMemoryAllocation = dynamicMemoryAllocation;
    }

    public void setDynamicMemoryAllocationRange(int dynamicMemoryAllocationRange) {
        this.dynamicMemoryAllocationRange = dynamicMemoryAllocationRange;
    }

    public void setPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture(int percentOfPlayersToCheckShouldAutoStopTheServiceInFuture) {
        this.percentOfPlayersToCheckShouldAutoStopTheServiceInFuture = percentOfPlayersToCheckShouldAutoStopTheServiceInFuture;
    }

    public void setAutoStopTimeByUnusedServiceInSeconds(int autoStopTimeByUnusedServiceInSeconds) {
        this.autoStopTimeByUnusedServiceInSeconds = autoStopTimeByUnusedServiceInSeconds;
    }

    public void setSwitchToPreparedServiceAfterAutoStopTimeByUnusedService(boolean switchToPreparedServiceAfterAutoStopTimeByUnusedService) {
        this.switchToPreparedServiceAfterAutoStopTimeByUnusedService = switchToPreparedServiceAfterAutoStopTimeByUnusedService;
    }

    public void setPercentOfPlayersForANewServiceByInstance(int percentOfPlayersForANewServiceByInstance) {
        this.percentOfPlayersForANewServiceByInstance = percentOfPlayersForANewServiceByInstance;
    }

    public void setForAnewInstanceDelayTimeInSeconds(int forAnewInstanceDelayTimeInSeconds) {
        this.forAnewInstanceDelayTimeInSeconds = forAnewInstanceDelayTimeInSeconds;
    }

    public void setTemplateInstaller(TemplateInstaller templateInstaller) {
        this.templateInstaller = templateInstaller;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SmartServiceTaskConfig)) return false;
        final SmartServiceTaskConfig other = (SmartServiceTaskConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$task = this.getTask();
        final Object other$task = other.getTask();
        if (this$task == null ? other$task != null : !this$task.equals(other$task)) return false;
        if (this.getPriority() != other.getPriority()) return false;
        if (this.isDirectTemplatesAndInclusionsSetup() != other.isDirectTemplatesAndInclusionsSetup()) return false;
        if (this.getPreparedServices() != other.getPreparedServices()) return false;
        if (this.getMinServiceOnlineCount() != other.getMinServiceOnlineCount()) return false;
        if (this.isDynamicMemoryAllocation() != other.isDynamicMemoryAllocation()) return false;
        if (this.getDynamicMemoryAllocationRange() != other.getDynamicMemoryAllocationRange()) return false;
        if (this.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture() != other.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture())
            return false;
        if (this.getAutoStopTimeByUnusedServiceInSeconds() != other.getAutoStopTimeByUnusedServiceInSeconds())
            return false;
        if (this.isSwitchToPreparedServiceAfterAutoStopTimeByUnusedService() != other.isSwitchToPreparedServiceAfterAutoStopTimeByUnusedService())
            return false;
        if (this.getPercentOfPlayersForANewServiceByInstance() != other.getPercentOfPlayersForANewServiceByInstance())
            return false;
        if (this.getForAnewInstanceDelayTimeInSeconds() != other.getForAnewInstanceDelayTimeInSeconds()) return false;
        final Object this$templateInstaller = this.getTemplateInstaller();
        final Object other$templateInstaller = other.getTemplateInstaller();
        if (this$templateInstaller == null ? other$templateInstaller != null : !this$templateInstaller.equals(other$templateInstaller))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SmartServiceTaskConfig;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $task = this.getTask();
        result = result * PRIME + ($task == null ? 43 : $task.hashCode());
        result = result * PRIME + this.getPriority();
        result = result * PRIME + (this.isDirectTemplatesAndInclusionsSetup() ? 79 : 97);
        result = result * PRIME + this.getPreparedServices();
        result = result * PRIME + this.getMinServiceOnlineCount();
        result = result * PRIME + (this.isDynamicMemoryAllocation() ? 79 : 97);
        result = result * PRIME + this.getDynamicMemoryAllocationRange();
        result = result * PRIME + this.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture();
        result = result * PRIME + this.getAutoStopTimeByUnusedServiceInSeconds();
        result = result * PRIME + (this.isSwitchToPreparedServiceAfterAutoStopTimeByUnusedService() ? 79 : 97);
        result = result * PRIME + this.getPercentOfPlayersForANewServiceByInstance();
        result = result * PRIME + this.getForAnewInstanceDelayTimeInSeconds();
        final Object $templateInstaller = this.getTemplateInstaller();
        result = result * PRIME + ($templateInstaller == null ? 43 : $templateInstaller.hashCode());
        return result;
    }

    public String toString() {
        return "SmartServiceTaskConfig(task=" + this.getTask() + ", priority=" + this.getPriority() + ", directTemplatesAndInclusionsSetup=" + this.isDirectTemplatesAndInclusionsSetup() + ", preparedServices=" + this.getPreparedServices() + ", minServiceOnlineCount=" + this.getMinServiceOnlineCount() + ", dynamicMemoryAllocation=" + this.isDynamicMemoryAllocation() + ", dynamicMemoryAllocationRange=" + this.getDynamicMemoryAllocationRange() + ", percentOfPlayersToCheckShouldAutoStopTheServiceInFuture=" + this.getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture() + ", autoStopTimeByUnusedServiceInSeconds=" + this.getAutoStopTimeByUnusedServiceInSeconds() + ", switchToPreparedServiceAfterAutoStopTimeByUnusedService=" + this.isSwitchToPreparedServiceAfterAutoStopTimeByUnusedService() + ", percentOfPlayersForANewServiceByInstance=" + this.getPercentOfPlayersForANewServiceByInstance() + ", forAnewInstanceDelayTimeInSeconds=" + this.getForAnewInstanceDelayTimeInSeconds() + ", templateInstaller=" + this.getTemplateInstaller() + ")";
    }
}