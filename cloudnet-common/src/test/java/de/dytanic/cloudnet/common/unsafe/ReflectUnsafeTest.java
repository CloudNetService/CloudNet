package de.dytanic.cloudnet.common.unsafe;

import org.junit.Assert;
import org.junit.Test;

public class ReflectUnsafeTest {

    @Test
    public void testReflectiveUnsafe()
    {
        Assert.assertNotNull(ReflectUnsafe.getUnsafe());
    }

}