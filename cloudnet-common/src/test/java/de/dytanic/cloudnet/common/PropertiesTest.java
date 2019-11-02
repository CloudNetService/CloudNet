package de.dytanic.cloudnet.common;

import org.junit.Assert;
import org.junit.Test;

public class PropertiesTest {

    @Test
    public void testPropertiesParser() {
        Properties properties = Properties.parseLine("test=true --foo -xfy");

        Assert.assertEquals(3, properties.size());
        Assert.assertTrue(properties.getBoolean("test"));
    }

}