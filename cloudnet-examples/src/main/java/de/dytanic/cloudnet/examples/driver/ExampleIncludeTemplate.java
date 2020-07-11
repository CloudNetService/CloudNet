package de.dytanic.cloudnet.examples.driver;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public final class ExampleIncludeTemplate {

    public void exampleIncludeTemplates(UUID playerUniqueId, ServiceInfoSnapshot serviceInfoSnapshot) {
        //Add serviceTemplate to existing service
        serviceInfoSnapshot.provider().addServiceTemplate(new ServiceTemplate("Lobby", "test1", "local"));

        //Create service with custom template
        ServiceInfoSnapshot newService = CloudNetDriver.getInstance().getCloudServiceFactory().createCloudService(
                "PS-" + playerUniqueId.toString(),
                "jvm",
                true,
                false,
                new ArrayList<>(),
                new ArrayList<>(Collections.singletonList(
                        new ServiceTemplate(
                                "Lobby", "test1",
                                "local"
                        )
                )),
                new ArrayList<>(),
                Collections.singletonList("PrivateServerGroup"),
                new ProcessConfiguration(
                        ServiceEnvironmentType.MINECRAFT_SERVER,
                        256,
                        new ArrayList<>()
                ),
                JsonDocument.newDocument().append("UUID", playerUniqueId), //define useful properties to call up later
                null
        );

        if (newService != null) {
            newService.provider().start();
        }
    }
}