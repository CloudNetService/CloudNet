package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class GroupConfiguraionSerializerTest {

  @Test
  public void serializeGroupConfiguration() {
    GroupConfiguration original = new GroupConfiguration(
      Collections.singletonList(new ServiceRemoteInclusion("https://cloudnetservice.eu", "destination")),
      Collections.singletonList(new ServiceTemplate("Global", "default", "local", true)),
      Collections.singletonList(new ServiceDeployment(new ServiceTemplate("Backup", "Global", "local", true),
        Arrays.asList("some", "excluded", "files"))),
      "Global",
      Arrays.asList("jvm", "options"),
      Arrays.asList(ServiceEnvironmentType.MINECRAFT_SERVER, ServiceEnvironmentType.BUNGEECORD,
        ServiceEnvironmentType.NUKKIT)
    );

    ProtocolBuffer buffer = ProtocolBuffer.create();
    buffer.writeObject(original);

    GroupConfiguration deserialized = buffer.readObject(GroupConfiguration.class);

    Assert.assertEquals(original, deserialized);
  }

}
