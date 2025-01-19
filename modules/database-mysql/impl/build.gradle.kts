/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

plugins {
  alias(libs.plugins.shadow)
}

tasks.withType<Jar> {
  archiveFileName.set(Files.databaseMysql)
}

dependencies {
  "moduleLibrary"(libs.bundles.mysql) {
    exclude("com.google.protobuf")
  }

  "compileOnly"(libs.caffeine)
  "compileOnly"(projects.node.nodeImpl)

  "api"(projects.modules.databaseMysql.databaseMysqlApi)

  "testCompileOnly"(libs.caffeine)
  "testImplementation"(projects.node.nodeImpl)
}

moduleJson {
  author = "CloudNetService"
  name = "CloudNet-Database-MySQL"
  main = "eu.cloudnetservice.modules.mysql.impl.CloudNetMySQLDatabaseModule"
  description = "CloudNet extension, which includes the database support for MySQL and MariaDB"
  minJavaVersionId = JavaVersion.VERSION_11
  runtimeModule = true
  storesSensitiveData = true
}
