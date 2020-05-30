package de.dytanic.cloudnet.driver.serialization;

import de.dytanic.cloudnet.driver.permission.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

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
