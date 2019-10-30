package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;

import java.util.Collection;

public interface GroupConfigurationProvider {

     /**
      * Gets all groups that are registered in the cloud
      *
      * @return a list containing the group configurations of all groups
      */
     Collection<GroupConfiguration> getGroupConfigurations();

     /**
      * Gets a specific group by its name
      *
      * @param name the name of the group
      * @return the group or {@code null} if no group with that name exists
      */
     GroupConfiguration getGroupConfiguration(String name);

     /**
      * Checks whether the group with a specific name exists
      *
      * @param name the name of the group
      * @return {@code true} if the group exists or {@code false} otherwise
      */
     boolean isGroupConfigurationPresent(String name);

     /**
      * Adds a new group to the cloud
      *
      * @param groupConfiguration the group to be added
      */
     void addGroupConfiguration(GroupConfiguration groupConfiguration);

     /**
      * Removes a group from the cloud
      *
      * @param name the name of the group to be removed
      */
     void removeGroupConfiguration(String name);

     /**
      * Removes a group from the cloud
      *
      * @param groupConfiguration the group to be removed (the only thing that matters in this object is the name, the rest is ignored)
      */
     void removeGroupConfiguration(GroupConfiguration groupConfiguration);

     /**
      * Gets all groups that are registered in the cloud
      *
      * @return a list containing the group configurations of all groups
      */
     ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync();

     /**
      * Gets a specific group by its name
      *
      * @param name the name of the group
      * @return the group or {@code null} if no group with that name exists
      */
     ITask<GroupConfiguration> getGroupConfigurationAsync(String name);

     /**
      * Checks whether the group with a specific name exists
      *
      * @param name the name of the group
      * @return {@code true} if the group exists or {@code false} otherwise
      */
     ITask<Boolean> isGroupConfigurationPresentAsync(String name);

     /**
      * Adds a new group to the cloud
      *
      * @param groupConfiguration the group to be added
      */
     ITask<Void> addGroupConfigurationAsync(GroupConfiguration groupConfiguration);

     /**
      * Removes a group from the cloud
      *
      * @param name the name of the group to be removed
      */
     ITask<Void> removeGroupConfigurationAsync(String name);

     /**
      * Removes a group from the cloud
      *
      * @param groupConfiguration the group to be removed (the only thing that matters in this object is the name, the rest is ignored)
      */
     ITask<Void> removeGroupConfigurationAsync(GroupConfiguration groupConfiguration);

}
