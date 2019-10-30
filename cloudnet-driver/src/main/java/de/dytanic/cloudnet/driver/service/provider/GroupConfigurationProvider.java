package de.dytanic.cloudnet.driver.service.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;

import java.util.Collection;

public interface GroupConfigurationProvider {
    
     Collection<GroupConfiguration> getGroupConfigurations();

     GroupConfiguration getGroupConfiguration(String name);

     boolean isGroupConfigurationPresent(String name);

     void addGroupConfiguration(GroupConfiguration groupConfiguration);

     void removeGroupConfiguration(String name);

     void removeGroupConfiguration(GroupConfiguration groupConfiguration);

     ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync();

     ITask<GroupConfiguration> getGroupConfigurationAsync(String name);

     ITask<Boolean> isGroupConfigurationPresentAsync(String name);

     ITask<Void> addGroupConfigurationAsync(GroupConfiguration groupConfiguration);

     ITask<Void> removeGroupConfigurationAsync(String name);

     ITask<Void> removeGroupConfigurationAsync(GroupConfiguration groupConfiguration);

}
