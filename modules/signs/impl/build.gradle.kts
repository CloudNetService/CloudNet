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
  archiveFileName.set(Files.signs)

  manifest {
    attributes["paperweight-mappings-namespace"] = "mojang"
  }
}

tasks.withType<JavaCompile> {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/signs.aero")
}

dependencies {
  "compileOnly"(libs.reflexion)

  "compileOnly"(projects.node.nodeImpl)
  "compileOnly"(projects.utils.utilsBase)
  "compileOnly"(projects.ext.adventureHelper)
  "compileOnly"(projects.wrapperJvm.wrapperJvmApi)
  "compileOnly"(projects.modules.bridge.bridgeApi)
  "compileOnly"(projects.modules.bridge.bridgeImpl)

  "implementation"(projects.ext.bukkitCommand)
  "implementation"(projects.modules.signs.signsApi)

  // processing
  "annotationProcessor"(libs.aerogelAuto)

  "compileOnly"(libs.bundles.serverPlatform)
}

moduleJson {
  name = "CloudNet-Signs"
  author = "CloudNetService"
  main = "eu.cloudnetservice.modules.signs.impl.node.CloudNetSignsModule"
  description = "CloudNet extension which adds sign connector support for Bukkit, Nukkit and Sponge"
  // depend on internal modules
  dependencies.add(ModuleConfiguration.Dependency("CloudNet-Bridge").apply {
    needsRepoResolve = false
    group = project.group.toString()
    version = project.version.toString()
  })
}
