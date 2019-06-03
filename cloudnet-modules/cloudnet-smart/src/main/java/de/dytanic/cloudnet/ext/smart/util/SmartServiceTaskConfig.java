package de.dytanic.cloudnet.ext.smart.util;

import de.dytanic.cloudnet.ext.smart.template.TemplateInstaller;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
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

    @Override
    public int compareTo(SmartServiceTaskConfig o)
    {
        return priority + o.priority;
    }
}