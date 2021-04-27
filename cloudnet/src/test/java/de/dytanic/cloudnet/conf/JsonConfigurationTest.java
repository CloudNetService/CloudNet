package de.dytanic.cloudnet.conf;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Random;

public class JsonConfigurationTest {

    static {
        System.setProperty("cloudnet.config.json.path", "build/config.json");
    }

    @Test
    public void testDocumentConfiguration() {
        IConfiguration configuration = new JsonConfiguration();
        configuration.load();

        int rand = new Random().nextInt();

        configuration.setMaxMemory(rand);
        configuration.setIpWhitelist(Collections.singletonList("0.0.1.0"));

        configuration.setMaxCPUUsageToStartServices(45);
        configuration.setDefaultJVMFlags(IConfiguration.DefaultJVMFlags.AIKAR);
        configuration.setHttpListeners(Collections.singletonList(new HostAndPort("127.0.1.1", 444)));
        configuration.setParallelServiceStartSequence(true);
        configuration.setRunBlockedServiceStartTryLaterAutomatic(false);

        configuration.load();

        Assert.assertTrue(configuration.getIpWhitelist().contains("0.0.1.0"));
        Assert.assertEquals(1, configuration.getIpWhitelist().size());
        Assert.assertEquals(configuration.getMaxMemory(), rand);

        Assert.assertEquals(configuration.getDefaultJVMFlags(), IConfiguration.DefaultJVMFlags.AIKAR);
        Assert.assertTrue(configuration.getHttpListeners().contains(new HostAndPort("127.0.1.1", 444)));
        Assert.assertTrue(configuration.isParallelServiceStartSequence());
        Assert.assertFalse(configuration.isRunBlockedServiceStartTryLaterAutomatic());
    }
}