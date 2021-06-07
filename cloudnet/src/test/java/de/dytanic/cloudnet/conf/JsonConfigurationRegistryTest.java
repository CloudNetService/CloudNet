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

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;

public final class JsonConfigurationRegistryTest {

  @Test
  public void testConfigurationRegistry() {
    IConfigurationRegistry configurationRegistry = new JsonConfigurationRegistry(Paths.get("build/registry.json"));

    Assert.assertNotNull(
      configurationRegistry
        .put("a", "Test String")
        .put("b", "foobar".getBytes())
        .put("c", 24)
        .put("d", new Person("Peter Parker", 24, new JsonDocument("address", new HostAndPort("127.0.0.1", 6533))))
    );

    Assert.assertTrue(configurationRegistry.contains("a"));
    Assert.assertTrue(configurationRegistry.contains("b"));
    Assert.assertTrue(configurationRegistry.contains("c"));
    Assert.assertTrue(configurationRegistry.contains("d"));

    Assert.assertFalse(configurationRegistry.contains("f"));

    Assert.assertNotNull(configurationRegistry.getString("a"));
    Assert.assertNotNull(configurationRegistry.getBytes("b"));
    Assert.assertNotNull(configurationRegistry.getInt("c"));
    Assert.assertNotNull(configurationRegistry.getObject("d", Person.class));

    Assert.assertNull(configurationRegistry.getString("f"));

    Assert.assertEquals("Test String", configurationRegistry.getString("a"));
    Assert.assertEquals("foobar", new String(configurationRegistry.getBytes("b")));
    Assert.assertEquals((Object) 24, configurationRegistry.getInt("c"));
    Assert.assertEquals((Object) ((short) 24), configurationRegistry.getShort("c"));
    Assert.assertEquals((Object) 24L, configurationRegistry.getLong("c"));

    Person person = configurationRegistry.getObject("d", Person.class);

    Assert.assertNotNull(person);
    Assert.assertNotNull(person.name);
    Assert.assertNotNull(person.properties);

    Assert.assertEquals("Peter Parker", person.name);
    Assert.assertEquals(24, person.age);

    Assert.assertTrue(person.properties.contains("address"));

    HostAndPort hostAndPort = person.properties.get("address", HostAndPort.class);
    Assert.assertNotNull(hostAndPort);

    Assert.assertEquals("127.0.0.1", hostAndPort.getHost());
    Assert.assertEquals(6533, hostAndPort.getPort());

    Assert.assertNotNull(configurationRegistry.remove("a"));
    Assert.assertFalse(configurationRegistry.contains("a"));
  }

  private static final class Person {

    private final JsonDocument properties;
    private final String name;
    private final int age;

    public Person(String name, int age, JsonDocument properties) {
      this.name = name;
      this.age = age;
      this.properties = properties;
    }
  }
}
