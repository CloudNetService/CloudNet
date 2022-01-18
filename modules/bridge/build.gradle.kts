/*
 * Copyright 2019-2022 CloudNetService team & contributors
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
  id("fabric-loom") version Versions.fabricLoom
  id("com.github.johnrengelman.shadow") version Versions.shadow
}

tasks.withType<Jar> {
  archiveFileName.set(Files.bridge)
}

dependencies {
  "compileOnly"(projects.cloudnetWrapperJvm)
  "compileOnly"(libs.bundles.proxyPlatform)
  "compileOnly"(libs.bundles.serverPlatform)

  "annotationProcessor"(libs.velocity)
  "implementation"(libs.bundles.adventure)
  "implementation"(projects.cloudnetExt.adventureHelper)

  "minecraft"(libs.minecraft)
  "mappings"(libs.yarnMappings)
  "modImplementation"(libs.fabricLoader)
}

loom {
  accessWidenerPath.set(project.file("src/main/resources/cloudnet_bridge.accesswidener"))
}

moduleJson {
  name = "CloudNet-Bridge"
  author = "CloudNetService"
  main = "eu.cloudnetservice.modules.bridge.node.CloudNetBridgeModule"
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
  // drop unused classes which are making the jar bigger
  minimize()
}

configure<net.kyori.blossom.BlossomExtension> {
  replaceToken("{project.build.version}", project.version)
}
