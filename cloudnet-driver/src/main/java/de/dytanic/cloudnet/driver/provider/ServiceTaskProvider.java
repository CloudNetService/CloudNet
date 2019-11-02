package de.dytanic.cloudnet.driver.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.util.Collection;

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
     ServiceTask getServiceTask(String name);

     /**
      * Checks whether the task with a specific name exists
      *
      * @param name the name of the task
      * @return {@code true} if the task exists or {@code false} otherwise
      */
     boolean isServiceTaskPresent(String name);

     /**
      * Adds a new task to the cloud
      *
      * @param serviceTask the task to be added
      */
     void addPermanentServiceTask(ServiceTask serviceTask);

     /**
      * Removes a task from the cloud
      *
      * @param name the name of the task to be removed
      */
     void removePermanentServiceTask(String name);

     /**
      * Removes a task from the cloud
      *
      * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is ignored)
      */
     void removePermanentServiceTask(ServiceTask serviceTask);

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
     ITask<ServiceTask> getServiceTaskAsync(String name);

     /**
      * Checks whether the task with a specific name exists
      *
      * @param name the name of the task
      * @return {@code true} if the task exists or {@code false} otherwise
      */
     ITask<Boolean> isServiceTaskPresentAsync(String name);

     /**
      * Adds a new task to the cloud
      *
      * @param serviceTask the task to be added
      */
     ITask<Void> addPermanentServiceTaskAsync(ServiceTask serviceTask);

     /**
      * Removes a task from the cloud
      *
      * @param name the name of the task to be removed
      */
     ITask<Void> removePermanentServiceTaskAsync(String name);

     /**
      * Removes a task from the cloud
      *
      * @param serviceTask the task to be removed (the only thing that matters in this object is the name, the rest is ignored)
      */
     ITask<Void> removePermanentServiceTaskAsync(ServiceTask serviceTask);
    
}
