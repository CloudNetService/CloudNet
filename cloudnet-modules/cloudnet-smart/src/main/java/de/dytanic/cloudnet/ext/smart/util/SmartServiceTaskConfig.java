package de.dytanic.cloudnet.ext.smart.util;

import de.dytanic.cloudnet.ext.smart.template.TemplateInstaller;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
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

    public void setTask(String task) {
        this.task = task;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDirectTemplatesAndInclusionsSetup() {
        return this.directTemplatesAndInclusionsSetup;
    }

    public void setDirectTemplatesAndInclusionsSetup(boolean directTemplatesAndInclusionsSetup) {
        this.directTemplatesAndInclusionsSetup = directTemplatesAndInclusionsSetup;
    }

    public int getPreparedServices() {
        return this.preparedServices;
    }

    public void setPreparedServices(int preparedServices) {
        this.preparedServices = preparedServices;
    }

    public int getMinServiceOnlineCount() {
        return this.minServiceOnlineCount;
    }

    public void setMinServiceOnlineCount(int minServiceOnlineCount) {
        this.minServiceOnlineCount = minServiceOnlineCount;
    }

    public boolean isDynamicMemoryAllocation() {
        return this.dynamicMemoryAllocation;
    }

    public void setDynamicMemoryAllocation(boolean dynamicMemoryAllocation) {
        this.dynamicMemoryAllocation = dynamicMemoryAllocation;
    }

    public int getDynamicMemoryAllocationRange() {
        return this.dynamicMemoryAllocationRange;
    }

    public void setDynamicMemoryAllocationRange(int dynamicMemoryAllocationRange) {
        this.dynamicMemoryAllocationRange = dynamicMemoryAllocationRange;
    }

    public int getPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture() {
        return this.percentOfPlayersToCheckShouldAutoStopTheServiceInFuture;
    }

    public void setPercentOfPlayersToCheckShouldAutoStopTheServiceInFuture(int percentOfPlayersToCheckShouldAutoStopTheServiceInFuture) {
        this.percentOfPlayersToCheckShouldAutoStopTheServiceInFuture = percentOfPlayersToCheckShouldAutoStopTheServiceInFuture;
    }

    public int getAutoStopTimeByUnusedServiceInSeconds() {
        return this.autoStopTimeByUnusedServiceInSeconds;
    }

    public void setAutoStopTimeByUnusedServiceInSeconds(int autoStopTimeByUnusedServiceInSeconds) {
        this.autoStopTimeByUnusedServiceInSeconds = autoStopTimeByUnusedServiceInSeconds;
    }

    public boolean isSwitchToPreparedServiceAfterAutoStopTimeByUnusedService() {
        return this.switchToPreparedServiceAfterAutoStopTimeByUnusedService;
    }

    public void setSwitchToPreparedServiceAfterAutoStopTimeByUnusedService(boolean switchToPreparedServiceAfterAutoStopTimeByUnusedService) {
        this.switchToPreparedServiceAfterAutoStopTimeByUnusedService = switchToPreparedServiceAfterAutoStopTimeByUnusedService;
    }

    public int getPercentOfPlayersForANewServiceByInstance() {
        return this.percentOfPlayersForANewServiceByInstance;
    }

    public void setPercentOfPlayersForANewServiceByInstance(int percentOfPlayersForANewServiceByInstance) {
        this.percentOfPlayersForANewServiceByInstance = percentOfPlayersForANewServiceByInstance;
    }

    public int getForAnewInstanceDelayTimeInSeconds() {
        return this.forAnewInstanceDelayTimeInSeconds;
    }

    public void setForAnewInstanceDelayTimeInSeconds(int forAnewInstanceDelayTimeInSeconds) {
        this.forAnewInstanceDelayTimeInSeconds = forAnewInstanceDelayTimeInSeconds;
    }

    public TemplateInstaller getTemplateInstaller() {
        return this.templateInstaller;
    }

    public void setTemplateInstaller(TemplateInstaller templateInstaller) {
        this.templateInstaller = templateInstaller;
    }

}