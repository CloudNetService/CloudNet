package de.dytanic.cloudnet.util;

import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;

public final class PortValidatorTest {

    @Test
    public void testCheckPort() {
        Assert.assertTrue(PortValidator.checkPort(45893));
    }

    @Test
    public void testCanAssignAddress() throws Exception {
        Assert.assertTrue(PortValidator.canAssignAddress(InetAddress.getLocalHost().getHostAddress()));
        Assert.assertFalse(PortValidator.canAssignAddress("999.999.999.999"));
        Assert.assertFalse(PortValidator.canAssignAddress("172.16.254.1"));
    }
}
