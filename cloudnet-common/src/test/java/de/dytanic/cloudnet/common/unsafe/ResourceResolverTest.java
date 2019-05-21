package de.dytanic.cloudnet.common.unsafe;

import org.junit.Assert;
import org.junit.Test;

public class ResourceResolverTest {

    @Test
    public void testResourceResolver()
    {
        Assert.assertNotNull(ResourceResolver.resolveURIFromResourceByClass(ResourceResolverTest.class));
    }
}