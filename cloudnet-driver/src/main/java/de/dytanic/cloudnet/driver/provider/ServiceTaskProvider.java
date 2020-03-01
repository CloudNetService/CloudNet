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
      * Gets all tasks that are registered in the cloud
      *
      * @return a list containing the task configurations of all tasks
      */
     Collection<ServiceTask> getPermanentServiceTasks();

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
    void addPermanentServiceTask(@NotNull ServiceTask serviceTask);

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
      * Gets all tasks that are registered in the cloud
      *
      * @return a list containing the task configurations of all tasks
      */
     ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync();

     /**
      * Gets a specific task by its name
      *
      * @param name the name of the task
      * @return the task or {@code null} if no task with that name exists
      */
     ITask<ServiceTask> getServiceTaskAsync(@NotNull String name);

    /**
     * Checks whether the task with a specific name exists
     *
     * @param name the name of the task
     * @return {@code true} if the task exists or {@code false} otherwise
     */
    ITask<Boolean> isServiceTaskPresentAsync(@NotNull String name);

    /**
     * Adds a new task to the cloud
     *
     * @param serviceTask the task to be added
     */
    ITask<Void> addPermanentServiceTaskAsync(@NotNull ServiceTask serviceTask);

    /**
     * Removes a task from the cloud
     *
     * @param name the name of the task to be removed
     */
    ITask<Void> removePermanentServiceTaskAsync(@NotNull String name);

    /**
     * Removes a task from the cloud
     *
     * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is ignored)
     */
    ITask<Void> removePermanentServiceTaskAsync(@NotNull ServiceTask serviceTask);

}
