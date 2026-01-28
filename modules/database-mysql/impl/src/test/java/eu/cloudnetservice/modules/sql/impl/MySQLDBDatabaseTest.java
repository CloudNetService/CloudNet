/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.sql.impl;

import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.modules.mysql.config.DatabaseType;
import eu.cloudnetservice.modules.mysql.config.JooqConfigurationEntry;
import eu.cloudnetservice.modules.sql.impl.junit.EnableServicesInject;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@EnableServicesInject
@Testcontainers(disabledWithoutDocker = true)
public class MySQLDBDatabaseTest extends SQLDatabaseTest {

  @Container
  private static final GenericContainer<?> mysqlContainer = new GenericContainer<>("mysql:latest")
    .withExposedPorts(3306)
    .withEnv("MYSQL_USER", "test")
    .withEnv("MYSQL_PASSWORD", "test")
    .withEnv("MYSQL_ROOT_PASSWORD", "test")
    .withEnv("MYSQL_DATABASE", "cn_testing");

  @BeforeAll
  static void setup() throws Exception {
    var config = new JooqConfigurationEntry(
      DatabaseType.MYSQL,
      "mysql",
      "cn_testing",
      "test",
      "test",
      new HostAndPort(mysqlContainer.getHost(), mysqlContainer.getFirstMappedPort()),
      null);
    databaseProvider = new JooqProvider(config);
    databaseProvider.init();
  }
}
