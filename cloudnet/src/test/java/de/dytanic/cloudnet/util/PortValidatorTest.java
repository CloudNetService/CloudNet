package de.dytanic.cloudnet.util;

import org.junit.Assert;
import org.junit.Test;

public final class PortValidatorTest {

    @Test
    public void testCheckPort() {
        Assert.assertTrue(PortValidator.checkPort(45893));
    }

    @Test
    public void testCanAssignAddress() {
        Assert.assertFalse(PortValidator.canAssignAddress("999.999.999.999"));
        Assert.assertFalse(PortValidator.canAssignAddress("172.16.254.1"));
    }
}
