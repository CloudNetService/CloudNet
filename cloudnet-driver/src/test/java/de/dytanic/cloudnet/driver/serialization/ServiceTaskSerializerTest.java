package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class ServiceTaskSerializerTest {

  @Test
  public void serializeServiceTask() {
    ServiceTask original = new ServiceTask(
      Collections.singletonList(new ServiceRemoteInclusion("https://cloudnetservice.eu", "destination")),
      Collections.singletonList(new ServiceTemplate("Global", "default", "local", true)),
      Collections.singletonList(new ServiceDeployment(new ServiceTemplate("Backup", "Global", "local", true),
        Arrays.asList("some", "excluded", "files"))),
      "Lobby",
      "jvm",
      true,
      true,
      true,
      Arrays.asList("Node-X", "Node-2"),
      Arrays.asList("Global", "Lobby", "Global-Server"),
      Arrays.asList("deleted", "files"),
      new ProcessConfiguration(ServiceEnvironmentType.MINECRAFT_SERVER, 1234, Arrays.asList("some", "options")),
      25565,
      5000
    );

    ProtocolBuffer buffer = ProtocolBuffer.create();
    buffer.writeObject(original);

    ServiceTask deserialized = buffer.readObject(ServiceTask.class);

    Assert.assertEquals(original, deserialized);
  }

}
