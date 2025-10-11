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
import eu.cloudnetservice.gradle.juppiter.ModuleConfiguration

plugins {
  id("cloudnet-modules")
  id("cloudnet-publish")
  alias(libs.plugins.shadow)
}

repositories {
  maven("https://repo.waterdog.dev/releases/")
  maven("https://repo.waterdog.dev/snapshots/")
  maven("https://repo.md-5.net/repository/releases/")
  maven("https://repo.md-5.net/repository/snapshots/")
  maven("https://repo.opencollab.dev/maven-releases/")
  maven("https://repo.opencollab.dev/maven-snapshots/")
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnly(libs.bundles.proxyPlatform)
  compileOnlyApi(projects.node.nodeImpl)
  compileOnlyApi(projects.wrapperJvm.wrapperJvmApi)
  compileOnlyApi(projects.modules.bridge.bridgeApi)

  compileOnly(projects.ext.adventureHelper)
  compileOnly(projects.ext.platformInjectSupport.platformInjectApi)

  api(projects.modules.syncproxy.syncproxyApi)

  annotationProcessor(libs.aerogelAuto)
  annotationProcessor(projects.ext.platformInjectSupport.platformInjectProcessor)
}

tasks.shadowJar.configure {
  archiveFileName = Files.syncproxy
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/syncproxy.aero")
}

moduleJson {
  author = "CloudNetService"
  name = "CloudNet-SyncProxy"
  main = "eu.cloudnetservice.modules.syncproxy.impl.node.CloudNetSyncProxyModule"
  description = "CloudNet extension which serves proxy utils with CloudNet support"
  // depend on internal modules
  dependencies.add(ModuleConfiguration.Dependency("CloudNet-Bridge").apply {
    needsRepoResolve = false
    group = project.group.toString()
    version = project.version.toString()
  })
}
