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

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class PermissionUserSerializerTest {

  @Test
  public void serializePermissionUser() {
    IPermissionUser original = new PermissionUser(
      UUID.randomUUID(),
      "derrop",
      null,
      Integer.MAX_VALUE
    );
    original.addPermission(new Permission("*", 999, 123456789L));
    original.addPermission("Proxy", new Permission("bungeecord.command.server", -999, -1));

    ProtocolBuffer buffer = ProtocolBuffer.create();
    buffer.writeObject(original);

    IPermissionUser deserialized = buffer.readObject(PermissionUser.class);

    Assert.assertEquals(original, deserialized);
  }

}
