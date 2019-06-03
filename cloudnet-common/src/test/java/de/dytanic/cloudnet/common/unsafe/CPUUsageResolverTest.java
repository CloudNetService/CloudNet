package de.dytanic.cloudnet.common.unsafe;

import org.junit.Assert;
import org.junit.Test;

public class CPUUsageResolverTest {

    @Test
    public void testCPUUsageResolver()
    {
        double value = CPUUsageResolver.getProcessCPUUsage(), system = CPUUsageResolver.getSystemCPUUsage();

        Assert.assertTrue(value <= 100D);
        Assert.assertTrue(system <= 100D);
    }
}