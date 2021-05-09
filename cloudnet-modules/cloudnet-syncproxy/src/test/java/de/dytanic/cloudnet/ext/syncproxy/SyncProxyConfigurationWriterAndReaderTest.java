package de.dytanic.cloudnet.ext.syncproxy;

import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationWriterAndReader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

public final class SyncProxyConfigurationWriterAndReaderTest {

    @Test
    public void testWriterAndReader() {
        SyncProxyConfiguration bungeeConfiguration = SyncProxyConfigurationWriterAndReader.read(Paths.get("build/sync_bungee.json"));

        Assert.assertNotNull(bungeeConfiguration);
        Assert.assertNotNull(bungeeConfiguration.getLoginConfigurations());
        Assert.assertNotNull(bungeeConfiguration.getTabListConfigurations());
    }
}