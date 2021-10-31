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

tasks.withType<Jar> {
  archiveFileName.set(Files.database_mongodb)
}

dependencies {
  "moduleLibrary"("org.mongodb", "mongodb-driver-sync", Versions.mongodb)
  "moduleLibrary"("org.slf4j", "slf4j-nop", Versions.slf4j)
}

moduleJson {
  main = "eu.cloudnetservice.cloudnet.ext.mongodb.CloudNetMongoDatabaseModule"
  author = "CloudNetService"
  description = "CloudNet extension, which includes the database support for MongoDB"
  runtimeModule = true
  storesSensitiveData = true
}
