package de.dytanic.cloudnet.network.listener.driver;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.api.DriverAPICategory;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.serialization.ProtocolBuffer;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.util.Collection;

public class DriverTemplateStorageListener extends CategorizedDriverAPIListener {
    public DriverTemplateStorageListener() {
        super(DriverAPICategory.TEMPLATE_STORAGE);

        super.registerHandler(DriverAPIRequestType.GET_TEMPLATE_STORAGE_TEMPLATES, (channel, packet, input) -> {
            Collection<ServiceTemplate> templates = CloudNetDriver.getInstance().getTemplateStorageTemplates(input.readString());
            return ProtocolBuffer.create().writeObjectCollection(templates);
        });

    }
}
