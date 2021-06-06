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
