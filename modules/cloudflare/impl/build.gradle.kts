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
  compileOnly(libs.bundles.unirest)
  compileOnlyApi(projects.node.nodeImpl)
  compileOnlyApi(projects.utils.utilsBase)
  api(projects.modules.cloudflare.cloudflareApi)
}

tasks.shadowJar.configure {
  archiveFileName = Files.cloudflare
}

moduleJson {
  author = "CloudNetService"
  name = "CloudNet-CloudFlare"
  main = "eu.cloudnetservice.modules.cloudflare.impl.CloudNetCloudflareModule"
  description = "Node extension for automatic creation of SRV entries for proxy services"
  storesSensitiveData = true
}
