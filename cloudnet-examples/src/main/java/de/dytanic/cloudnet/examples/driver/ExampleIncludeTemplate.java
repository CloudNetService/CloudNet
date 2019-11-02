package de.dytanic.cloudnet.examples.driver;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.util.Collections;
import java.util.UUID;

public final class ExampleIncludeTemplate {

    public void exampleIncludeTemplates(UUID playerUniqueId, ServiceInfoSnapshot serviceInfoSnapshot) {
        //Add serviceTemplate to existing service
        CloudNetDriver.getInstance().getCloudServiceProvider(serviceInfoSnapshot).addServiceTemplate(new ServiceTemplate(
                "Lobby", "test1",
                "local"
        ));

        //Create service with custom template
        ServiceInfoSnapshot newService = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(
                "PS-" + playerUniqueId.toString(),
                "jvm",
                true,
                false,
                Iterables.newArrayList(),
                Iterables.newArrayList(new ServiceTemplate[]{
                        new ServiceTemplate(
                                "Lobby", "test1",
                                "local"
                        )
                }),
                Iterables.newArrayList(),
                Collections.singletonList("PrivateServerGroup"),
                new ProcessConfiguration(
                        ServiceEnvironmentType.MINECRAFT_SERVER,
                        256,
                        Iterables.newArrayList()
                ),
                JsonDocument.newDocument().append("UUID", playerUniqueId), //define useful properties to call up later
                null
        );

        CloudNetDriver.getInstance().getCloudServiceProvider(newService).start();
    }
}