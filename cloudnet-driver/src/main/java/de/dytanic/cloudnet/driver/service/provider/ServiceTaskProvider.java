package de.dytanic.cloudnet.driver.service.provider;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.util.Collection;

public interface ServiceTaskProvider {

     Collection<ServiceTask> getPermanentServiceTasks();

     ServiceTask getServiceTask(String name);

     boolean isServiceTaskPresent(String name);

     void addPermanentServiceTask(ServiceTask serviceTask);

     void removePermanentServiceTask(String name);

     void removePermanentServiceTask(ServiceTask serviceTask);

     ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync();

     ITask<ServiceTask> getServiceTaskAsync(String name);

     ITask<Boolean> isServiceTaskPresentAsync(String name);
     
     ITask<Void> addPermanentServiceTaskAsync(ServiceTask serviceTask);

     ITask<Void> removePermanentServiceTaskAsync(String name);

     ITask<Void> removePermanentServiceTaskAsync(ServiceTask serviceTask);
    
}
