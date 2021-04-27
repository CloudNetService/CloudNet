package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.driver.service.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class GroupConfiguraionSerializerTest {

    @Test
    public void serializeGroupConfiguration() {
        GroupConfiguration original = new GroupConfiguration(
                Collections.singletonList(new ServiceRemoteInclusion("https://cloudnetservice.eu", "destination")),
                Collections.singletonList(new ServiceTemplate("Global", "default", "local", true)),
                Collections.singletonList(new ServiceDeployment(new ServiceTemplate("Backup", "Global", "local", true), Arrays.asList("some", "excluded", "files"))),
                "Global",
                Arrays.asList("jvm", "options"),
                Arrays.asList(ServiceEnvironmentType.MINECRAFT_SERVER, ServiceEnvironmentType.BUNGEECORD, ServiceEnvironmentType.NUKKIT)
        );

        ProtocolBuffer buffer = ProtocolBuffer.create();
        buffer.writeObject(original);

        GroupConfiguration deserialized = buffer.readObject(GroupConfiguration.class);

        Assert.assertEquals(original, deserialized);
    }

}
