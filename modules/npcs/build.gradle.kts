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
  archiveFileName.set(Files.npcs)
}

dependencies {
  "compileOnly"(projects.wrapperJvm)
  "compileOnly"(projects.modules.bridge)
  "implementation"(projects.ext.bukkitCommand)

  "compileOnly"(libs.bundles.serverPlatform)

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
