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

import eu.cloudnetservice.cloudnet.gradle.util.Files

plugins {
  id("cloudnet-modules")
  id("cloudnet-publish")
  alias(libs.plugins.shadow)
}

dependencies {
  moduleLibrary(libs.bundles.mysql) {
    exclude("com.google.protobuf")
  }

  compileOnly(libs.caffeine)
  compileOnlyApi(projects.node.nodeImpl)
  api(projects.modules.databaseMysql.databaseMysqlApi)

  implementation("org.jooq:jooq:3.20.10")
  implementation("org.xerial:sqlite-jdbc:3.51.1.0")
  implementation("org.postgresql:postgresql:42.7.9")
  implementation("com.mysql:mysql-connector-j:9.5.0")
  implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")
}

tasks.shadowJar.configure {
  archiveFileName = Files.databaseMysql
}

moduleJson {
  author = "CloudNetService"
  name = "CloudNet-Database-MySQL"
  main = "eu.cloudnetservice.modules.sql.impl.CloudNetMySQLDatabaseModule"
  description = "CloudNet extension, which includes the database support for MySQL and MariaDB"
  minJavaVersionId = JavaVersion.VERSION_11
  runtimeModule = true
  storesSensitiveData = true
}
