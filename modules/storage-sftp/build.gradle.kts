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

tasks.withType<Jar> {
  archiveFileName.set(Files.storageSftp)
}

dependencies {
  "moduleLibrary"(libs.sshj)
  "moduleLibrary"(libs.slf4jNop)
}

moduleJson {
  author = "CloudNetService"
  name = "CloudNet-Storage-SFTP"
  main = "eu.cloudnetservice.modules.sftp.SFTPTemplateStorageModule"
  description = "CloudNet extension, which includes the sftp storage system"
  storesSensitiveData = true
  runtimeModule = true
}
