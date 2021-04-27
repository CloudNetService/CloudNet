package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * This class provides access to the tasks of the cloud (tasks folder)
 */
public interface ServiceTaskProvider {

    /**
     * Reloads all tasks
     */
    void reload();

    /**
     * Gets all tasks that are registered in the cloud
     *
     * @return a list containing the task configurations of all tasks
     */
    Collection<ServiceTask> getPermanentServiceTasks();

    /**
     * Clears all existing service tasks and sets the given collection as the new service tasks
     *
     * @param serviceTasks the new service tasks
     */
    void setPermanentServiceTasks(@NotNull Collection<ServiceTask> serviceTasks);

    /**
     * Gets a specific task by its name
     *
     * @param name the name of the task
     * @return the task or {@code null} if no task with that name exists
     */
    @Nullable
    ServiceTask getServiceTask(@NotNull String name);

    /**
     * Checks whether the task with a specific name exists
     *
     * @param name the name of the task
     * @return {@code true} if the task exists or {@code false} otherwise
     */
    boolean isServiceTaskPresent(@NotNull String name);

    /**
     * Adds a new task to the cloud
     *
     * @param serviceTask the task to be added
     */
    boolean addPermanentServiceTask(@NotNull ServiceTask serviceTask);

    /**
     * Removes a task from the cloud
     *
     * @param name the name of the task to be removed
     */
    void removePermanentServiceTask(@NotNull String name);

    /**
     * Removes a task from the cloud
     *
     * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is ignored)
     */
    void removePermanentServiceTask(@NotNull ServiceTask serviceTask);

    /**
     * Reloads all tasks
     */
    @NotNull
    ITask<Void> reloadAsync();

    /**
     * Gets all tasks that are registered in the cloud
     *
     * @return a list containing the task configurations of all tasks
     */
    @NotNull
    ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync();

    /**
     * Clears all existing service tasks and sets the given collection as the new service tasks
     *
     * @param serviceTasks the new service tasks
     */
    @NotNull
    ITask<Void> setPermanentServiceTasksAsync(@NotNull Collection<ServiceTask> serviceTasks);

    /**
     * Gets a specific task by its name
     *
     * @param name the name of the task
     * @return the task or {@code null} if no task with that name exists
     */
    @NotNull
    ITask<ServiceTask> getServiceTaskAsync(@NotNull String name);

    /**
     * Checks whether the task with a specific name exists
     *
     * @param name the name of the task
     * @return {@code true} if the task exists or {@code false} otherwise
     */
    @NotNull
    ITask<Boolean> isServiceTaskPresentAsync(@NotNull String name);

    /**
     * Adds a new task to the cloud
     *
     * @param serviceTask the task to be added
     */
    @NotNull
    ITask<Boolean> addPermanentServiceTaskAsync(@NotNull ServiceTask serviceTask);

    /**
     * Removes a task from the cloud
     *
     * @param name the name of the task to be removed
     */
    @NotNull
    ITask<Void> removePermanentServiceTaskAsync(@NotNull String name);

    /**
     * Removes a task from the cloud
     *
     * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is ignored)
     */
    @NotNull
    ITask<Void> removePermanentServiceTaskAsync(@NotNull ServiceTask serviceTask);

}
