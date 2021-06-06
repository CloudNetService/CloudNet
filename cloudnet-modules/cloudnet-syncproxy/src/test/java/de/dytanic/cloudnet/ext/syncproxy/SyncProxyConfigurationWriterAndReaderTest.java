package de.dytanic.cloudnet.ext.syncproxy;

import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationWriterAndReader;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;

public final class SyncProxyConfigurationWriterAndReaderTest {

  @Test
  public void testWriterAndReader() {
    SyncProxyConfiguration bungeeConfiguration = SyncProxyConfigurationWriterAndReader
      .read(Paths.get("build/sync_bungee.json"));

    Assert.assertNotNull(bungeeConfiguration);
    Assert.assertNotNull(bungeeConfiguration.getLoginConfigurations());
    Assert.assertNotNull(bungeeConfiguration.getTabListConfigurations());
  }
}
