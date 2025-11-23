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

dependencies {
  compileOnly(libs.guava)
  compileOnlyApi(projects.node.nodeImpl)
  compileOnlyApi(projects.utils.utilsBase)
  compileOnlyApi(projects.modules.bridge.bridgeApi)

  annotationProcessor(libs.aerogelAuto)

  api(projects.modules.smart.smartApi)
}

tasks.shadowJar.configure {
  archiveFileName = Files.smart
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/smart.aero")
}

moduleJson {
  name = "CloudNet-Smart"
  author = "CloudNetService"
  main = "eu.cloudnetservice.modules.smart.impl.CloudNetSmartModule"
  description = "CloudNet extension, which implement smart network handling and automatic services providing"
  runtimeModule = true
  // depend on internal modules
  dependencies.add(ModuleConfiguration.Dependency("CloudNet-Bridge").apply {
    needsRepoResolve = false
    group = project.group.toString()
    version = project.version.toString()
  })
}
