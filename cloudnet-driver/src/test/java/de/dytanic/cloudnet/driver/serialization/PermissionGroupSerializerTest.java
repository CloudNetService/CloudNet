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

import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class PermissionGroupSerializerTest {

  @Test
  public void serializePermissionGroup() {
    IPermissionGroup original = new PermissionGroup(
      "Admin",
      Integer.MIN_VALUE,
      Arrays.asList("Moderator", "Team"),
      " \\ Admin \\ ",
      "§c",
      " / Team /",
      "Administrator | ",
      1,
      false
    );
    original.addPermission(new Permission("*", 999, 123456789L));
    original.addPermission("Proxy", new Permission("bungeecord.command.server", -999, -1));

    ProtocolBuffer buffer = ProtocolBuffer.create();
    buffer.writeObject(original);

    IPermissionGroup deserialized = buffer.readObject(PermissionGroup.class);

    Assert.assertEquals(original, deserialized);
  }

}
