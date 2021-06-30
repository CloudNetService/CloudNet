/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
