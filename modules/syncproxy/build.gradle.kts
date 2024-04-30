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

import eu.cloudnetservice.gradle.juppiter.ModuleConfiguration

plugins {
  alias(libs.plugins.shadow)
}

tasks.withType<Jar> {
  archiveFileName.set(Files.syncproxy)
}

tasks.withType<JavaCompile> {
  options.compilerArgs = mutableListOf("-AaerogelAutoFileName=autoconfigure/syncproxy.aero")
}

dependencies {
  "compileOnly"(libs.bundles.proxyPlatform)
  "compileOnly"(projects.wrapperJvm)
  "compileOnly"(projects.modules.bridge)

  // processing
  "annotationProcessor"(libs.aerogelAuto)

  "implementation"(projects.ext.adventureHelper)
}

moduleJson {
  author = "CloudNetService"
  name = "CloudNet-SyncProxy"
  main = "eu.cloudnetservice.modules.syncproxy.node.CloudNetSyncProxyModule"
  description = "CloudNet extension which serves proxy utils with CloudNet support"
  // depend on internal modules
  dependencies.add(ModuleConfiguration.Dependency("CloudNet-Bridge").apply {
    needsRepoResolve = false
    group = project.group.toString()
    version = project.version.toString()
  })
}
