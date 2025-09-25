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
  alias(libs.plugins.shadow)
}

repositories {
  maven("https://repo.codemc.io/repository/maven-releases/") {
    mavenContent {
      includeGroup("com.github.retrooper")
    }
  }
  maven("https://repo.codemc.io/repository/maven-snapshots/") {
    mavenContent {
      includeGroup("com.github.retrooper")
    }
  }
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
  compileOnly(libs.spigot)
  compileOnly(libs.unirest)
  compileOnly(libs.reflexion)
  compileOnly(projects.node.nodeImpl)
  compileOnly(projects.utils.utilsBase)
  compileOnly(projects.wrapperJvm.wrapperJvmApi)
  compileOnly(projects.modules.bridge.bridgeImpl)
  compileOnly(projects.ext.platformInjectSupport.platformInjectApi)

  implementation(libs.packetEvents)
  implementation(projects.ext.bukkitCommand)

  api(libs.bundles.npcLib)
  api(projects.modules.npcs.npcsApi)

  annotationProcessor(libs.aerogelAuto)
  annotationProcessor(projects.ext.platformInjectSupport.platformInjectProcessor)
}

tasks.shadowJar.configure {
  archiveFileName = Files.npcs

  relocate("net.kyori", "eu.cloudnetservice.modules.npc.relocate.net.kyori")
  relocate("io.papermc.lib", "eu.cloudnetservice.modules.npc.relocate.paperlib")
  relocate("io.leangen.geantyref", "eu.cloudnetservice.modules.npc.relocate.geantyref")
  relocate("io.github.retrooper", "eu.cloudnetservice.modules.npc.relocate.io.packetevents")
  relocate("com.github.retrooper", "eu.cloudnetservice.modules.npc.relocate.com.packetevents")
  relocate("com.github.juliarn.npclib", "eu.cloudnetservice.modules.npc.relocate.com.github.juliarn.npclib")

  dependencies {
    exclude("plugin.yml")
    exclude("META-INF/**", "**/*.html", "module-info.*")
  }

  manifest {
    attributes["paperweight-mappings-namespace"] = "mojang"
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/npcs.aero")
}

moduleJson {
  name = "CloudNet-NPCs"
  author = "CloudNetService"
  main = "eu.cloudnetservice.modules.npc.impl.node.CloudNetNPCModule"
  description = "CloudNet extension which adds NPCs for server selection"
  // depend on internal modules
  dependencies.add(ModuleConfiguration.Dependency("CloudNet-Bridge").apply {
    needsRepoResolve = false
    group = project.group.toString()
    version = project.version.toString()
  })
}
