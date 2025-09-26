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

import eu.cloudnetservice.cloudnet.gradle.util.Files
import eu.cloudnetservice.gradle.juppiter.ModuleConfiguration

plugins {
  id("cloudnet-modules")
  id("cloudnet-publish")
  alias(libs.plugins.shadow)
}

repositories {
  maven("https://repo.opencollab.dev/maven-releases/")
  maven("https://repo.opencollab.dev/maven-snapshots/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
  compileOnly(libs.spigot)
  compileOnly(libs.sponge)
  compileOnly(libs.nukkitX)
  compileOnly(libs.minestom)
  compileOnly(libs.reflexion)
  compileOnly(libs.minestomExtensions)

  compileOnlyApi(projects.node.nodeImpl)
  compileOnlyApi(projects.utils.utilsBase)
  compileOnlyApi(projects.wrapperJvm.wrapperJvmApi)
  compileOnlyApi(projects.modules.bridge.bridgeImpl)

  compileOnly(projects.ext.adventureHelper)
  compileOnly(projects.ext.platformInjectSupport.platformInjectApi)

  api(projects.modules.signs.signsApi)
  implementation(projects.ext.bukkitCommand)

  annotationProcessor(libs.aerogelAuto)
  annotationProcessor(projects.ext.platformInjectSupport.platformInjectProcessor)
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/signs.aero")
}

tasks.shadowJar.configure {
  archiveFileName = Files.signs

  manifest {
    attributes["paperweight-mappings-namespace"] = "mojang"
  }
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
