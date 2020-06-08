package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTask;

import java.util.Collection;

public class DriverServiceTaskListener extends CategorizedDriverAPIListener {
    public DriverServiceTaskListener() {
        super(DriverAPICategory.SERVICE_TASKS);

        super.registerHandler(DriverAPIRequestType.ADD_PERMANENT_SERVICE_TASK, (channel, packet, input) -> {
            CloudNetDriver.getInstance().getServiceTaskProvider().addPermanentServiceTask(input.readObject(ServiceTask.class));
            return ProtocolBuffer.EMPTY;
        });

        super.registerHandler(DriverAPIRequestType.REMOVE_PERMANENT_SERVICE_TASK, (channel, packet, input) -> {
            CloudNetDriver.getInstance().getServiceTaskProvider().removePermanentServiceTask(input.readString());
            return ProtocolBuffer.EMPTY;
        });

        super.registerHandler(DriverAPIRequestType.GET_PERMANENT_SERVICE_TASKS, (channel, packet, input) -> {
            Collection<ServiceTask> tasks = CloudNetDriver.getInstance().getServiceTaskProvider().getPermanentServiceTasks();
            return ProtocolBuffer.create().writeObjectCollection(tasks);
        });

        super.registerHandler(DriverAPIRequestType.GET_PERMANENT_SERVICE_TASK_BY_NAME, (channel, packet, input) -> {
            ServiceTask task = CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTask(input.readString());
            return ProtocolBuffer.create().writeOptionalObject(task);
        });

        super.registerHandler(DriverAPIRequestType.IS_SERVICE_TASK_PRESENT, (channel, packet, input) -> {
            boolean present = CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent(input.readString());
            return ProtocolBuffer.create().writeBoolean(present);
        });

    }
}
