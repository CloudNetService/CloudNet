package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * This class provides access to the groups of the cloud (groups.json file).
 */
public interface GroupConfigurationProvider {

    /**
     * Reloads the groups.json file
     */
    void reload();

    /**
     * Gets all groups that are registered in the cloud
     *
     * @return a list containing the group configurations of all groups
     */
    Collection<GroupConfiguration> getGroupConfigurations();

    /**
     * Clears all existing groups and sets the given collection as the new groups
     *
     * @param groupConfigurations the new groups
     */
    void setGroupConfigurations(@NotNull Collection<GroupConfiguration> groupConfigurations);

    /**
     * Gets a specific group by its name
     *
     * @param name the name of the group
     * @return the group or {@code null} if no group with that name exists
     */
    @Nullable
    GroupConfiguration getGroupConfiguration(@NotNull String name);

    /**
     * Checks whether the group with a specific name exists
     *
     * @param name the name of the group
     * @return {@code true} if the group exists or {@code false} otherwise
     */
    boolean isGroupConfigurationPresent(@NotNull String name);

    /**
     * Adds a new group to the cloud
     *
     * @param groupConfiguration the group to be added
     */
    void addGroupConfiguration(@NotNull GroupConfiguration groupConfiguration);

    /**
     * Removes a group from the cloud
     *
     * @param name the name of the group to be removed
     */
    void removeGroupConfiguration(@NotNull String name);

    /**
     * Removes a group from the cloud
     *
     * @param groupConfiguration the group to be removed (the only thing that matters in this object is the name, the rest is ignored)
     */
    void removeGroupConfiguration(@NotNull GroupConfiguration groupConfiguration);

    /**
     * Reloads the groups.json file
     */
    @NotNull
    ITask<Void> reloadAsync();

    /**
     * Gets all groups that are registered in the cloud
     *
     * @return a list containing the group configurations of all groups
     */
    @NotNull
    ITask<Collection<GroupConfiguration>> getGroupConfigurationsAsync();

    /**
     * Clears all existing groups and sets the given collection as the new groups
     *
     * @param groupConfigurations the new groups
     */
    @NotNull
    ITask<Void> setGroupConfigurationsAsync(@NotNull Collection<GroupConfiguration> groupConfigurations);

    /**
     * Gets a specific group by its name
     *
     * @param name the name of the group
     * @return the group or {@code null} if no group with that name exists
     */
    @NotNull
    ITask<GroupConfiguration> getGroupConfigurationAsync(@NotNull String name);

    /**
     * Checks whether the group with a specific name exists
     *
     * @param name the name of the group
     * @return {@code true} if the group exists or {@code false} otherwise
     */
    @NotNull
    ITask<Boolean> isGroupConfigurationPresentAsync(@NotNull String name);

    /**
     * Adds a new group to the cloud
     *
     * @param groupConfiguration the group to be added
     */
    @NotNull
    ITask<Void> addGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration);

    /**
     * Removes a group from the cloud
     *
     * @param name the name of the group to be removed
     */
    @NotNull
    ITask<Void> removeGroupConfigurationAsync(@NotNull String name);

    /**
     * Removes a group from the cloud
     *
     * @param groupConfiguration the group to be removed (the only thing that matters in this object is the name, the rest is ignored)
     */
    @NotNull
    ITask<Void> removeGroupConfigurationAsync(@NotNull GroupConfiguration groupConfiguration);

}
