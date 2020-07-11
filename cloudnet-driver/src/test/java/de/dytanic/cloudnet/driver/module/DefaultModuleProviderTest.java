package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.driver.module.defaults.DefaultModuleProvider;
import org.junit.Assert;
import org.junit.Test;

public final class DefaultModuleProviderTest {

    @Test
    public void testModule() throws Throwable {
        IModuleProvider moduleProvider = new DefaultModuleProvider(false, null);

        IModuleWrapper moduleWrapper = moduleProvider.loadModule(DefaultModuleProviderTest.class.getClassLoader().getResource("module.jar"));
        Assert.assertNull(moduleProvider.loadModule(DefaultModuleProviderTest.class.getClassLoader().getResource("module.jar")));

        Assert.assertNotNull(moduleWrapper);
        Assert.assertNotNull(System.getProperty("module_test_state"));
        Assert.assertEquals("loaded", System.getProperty("module_test_state"));

        Assert.assertNotNull(moduleWrapper.startModule());
        Assert.assertEquals("started", System.getProperty("module_test_state"));

        Assert.assertNotNull(moduleWrapper.stopModule());
        Assert.assertEquals("stopped", System.getProperty("module_test_state"));

        Assert.assertNotNull(moduleWrapper.unloadModule());
        Assert.assertEquals("unloaded", System.getProperty("module_test_state"));

        Assert.assertEquals(0, moduleProvider.getModules().size());
    }
}