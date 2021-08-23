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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ProcessSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.service.ThreadSnapshot;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class ServiceInfoSnapshotSerializerTest {

  @Test
  public void serializeServiceInfoSnapshot() {
    ServiceInfoSnapshot original = new ServiceInfoSnapshot(
      12345L,
      new HostAndPort("127.0.0.1", 25565),
      54321L,
      ServiceLifeCycle.RUNNING,
      new ProcessSnapshot(
        6789L,
        9876L,
        512_000_000,
        123987,
        12345L,
        54321L,
        Arrays.asList(
          new ThreadSnapshot(1, "snapshot1", Thread.State.BLOCKED, false, -1),
          new ThreadSnapshot(50, "snapshot2", Thread.State.RUNNABLE, true, -2)
        ),
        50.4D,
        456987
      ),
      JsonDocument.newDocument("key", "val"),
      new ServiceConfiguration(
        new ServiceId(UUID.fromString("fdef0011-1c58-40c8-bfef-0bdcb1495938"), "Node-1", "Lobby", 1,
          ServiceEnvironmentType.MINECRAFT_SERVER),
        "jvm",
        true,
        false,
        new String[]{"Lobby", "Global-Server"},
        new ServiceRemoteInclusion[]{new ServiceRemoteInclusion("https://cloudnetservice.eu", "destination")},
        new ServiceTemplate[]{new ServiceTemplate("Lobby", "default", "local", true)},
        new ServiceDeployment[]{new ServiceDeployment(new ServiceTemplate("Backup", "Lobby", "local", true),
          Arrays.asList("some", "excluded", "files"))},
        new String[]{"these", "files", "should", "be", "deleted"},
        new ProcessConfiguration(ServiceEnvironmentType.MINECRAFT_SERVER, 512, Arrays.asList("j", "v", "m", "options")),
        JsonDocument.newDocument("derpeepoMode", true),
        6789876
      )
    );

    ProtocolBuffer buffer = ProtocolBuffer.create();
    buffer.writeObject(original);

    ServiceInfoSnapshot deserialized = buffer.readObject(ServiceInfoSnapshot.class);

    Assert.assertEquals(original, deserialized);
  }

}
