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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  alias(libs.plugins.shadow)
}

tasks.withType<ShadowJar> {
  dependsOn(":cloudnet-common:jar", ":cloudnet-common:javadocJar", ":cloudnet-common:sourcesJar")

  archiveFileName.set(Files.wrapper)
  archiveVersion.set(null as String?)

  // netty relocation
  relocate("io.netty", "eu.cloudnetservice.relocate.io.netty")
  relocate("META-INF/native/netty", "META-INF/native/eu_cloudnetservice_relocate_netty")
  relocate("META-INF/native/libnetty", "META-INF/native/libeu_cloudnetservice_relocate_netty")

  // google lib relocation
  relocate("com.google.gson", "eu.cloudnetservice.relocate.gson")
  relocate("com.google.common", "eu.cloudnetservice.relocate.guava")

  // asm relocation to fix forge discovery:
  // https://github.com/MinecraftForge/MinecraftForge/blob/1.16.x/src/fmllauncher/java/net/minecraftforge/fml/loading/LibraryFinder.java#L39
  relocate("org.objectweb.asm.Opcodes", "eu.cloudnetservice.relocate.asm.Opcodes")

  // drop unused classes which are making the jar bigger
  minimize()

  doFirst {
    from(exportLanguageFileInformation())
  }
}

dependencies {
  "api"(projects.cloudnetDriver)
  "api"(projects.cloudnetExt.modlauncher)
}

applyJarMetadata(
  "eu.cloudnetservice.cloudnet.wrapper.Main",
  "eu.cloudnetservice.wrapper",
  "eu.cloudnetservice.cloudnet.wrapper.Premain")
