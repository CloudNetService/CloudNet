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
  archiveFileName.set(Files.storageS3)
}

dependencies {
  "moduleLibrary"(libs.awsSdk)
  "compileOnly"(projects.utils.utilsBase)
  "implementation"(projects.modules.storageS3.storageS3Api)

  "testImplementation"(projects.utils.utilsBase)
}

moduleJson {
  author = "CloudNetService"
  name = "CloudNet-Storage-S3"
  main = "eu.cloudnetservice.modules.s3.impl.S3TemplateStorageModule"
  description = "CloudNet extension, which includes the s3 storage system"
  runtimeModule = true
  storesSensitiveData = true
}
