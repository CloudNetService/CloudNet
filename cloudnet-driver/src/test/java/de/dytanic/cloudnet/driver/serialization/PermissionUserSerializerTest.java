package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.driver.permission.Permission;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.driver.service.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

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
