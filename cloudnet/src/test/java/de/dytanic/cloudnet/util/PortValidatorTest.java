package de.dytanic.cloudnet.util;

import org.junit.Assert;
import org.junit.Test;

public final class PortValidatorTest {

    @Test
    public void testPortValidator()
    {
        int port = 45893;

        Assert.assertTrue(PortValidator.checkPort(port));
    }
}