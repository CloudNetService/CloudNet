/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.conf;

import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.util.Collections;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

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
