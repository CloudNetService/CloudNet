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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import eu.cloudnetservice.gradle.juppiter.ModuleConfiguration

plugins {
  alias(libs.plugins.shadow)
}

tasks.withType<Jar> {
  archiveFileName.set(Files.npcs)

  manifest {
    attributes["paperweight-mappings-namespace"] = "mojang"
  }
}

tasks.withType<ShadowJar> {
  relocate("net.kyori", "eu.cloudnetservice.modules.npc.relocate.net.kyori")
  relocate("io.papermc.lib", "eu.cloudnetservice.modules.npc.relocate.paperlib")
  relocate("io.leangen.geantyref", "eu.cloudnetservice.modules.npc.relocate.geantyref")
  relocate("io.github.retrooper", "eu.cloudnetservice.modules.npc.relocate.io.packetevents")
  relocate("com.github.retrooper", "eu.cloudnetservice.modules.npc.relocate.com.packetevents")
  relocate("com.github.juliarn.npclib", "eu.cloudnetservice.modules.npc.relocate.com.github.juliarn.npclib")

  archiveClassifier.set(null as String?)
  dependencies {
    exclude("plugin.yml")
    // excludes the META-INF directory, module infos & html files of all dependencies
    // this includes for example maven lib files & multi-release module-json files
    exclude("META-INF/**", "**/*.html", "module-info.*")
  }
}

repositories {
  maven("https://repo.codemc.io/repository/maven-releases/") {
    mavenContent {
      includeGroup("com.github.retrooper")
    }
  }
}

dependencies {
  "compileOnly"(projects.wrapperJvm)
  "compileOnly"(projects.modules.bridge)
  "compileOnly"(libs.bundles.serverPlatform)

  "implementation"(libs.packetEvents)
  "implementation"(projects.ext.bukkitCommand)

  "api"(libs.bundles.npcLib)
}

moduleJson {
  name = "CloudNet-NPCs"
  author = "CloudNetService"
  main = "eu.cloudnetservice.modules.npc.node.CloudNetNPCModule"
  description = "CloudNet extension which adds NPCs for server selection"
  // depend on internal modules
  dependencies.add(ModuleConfiguration.Dependency("CloudNet-Bridge").apply {
    needsRepoResolve = false
    group = project.group.toString()
    version = project.version.toString()
  })
}
