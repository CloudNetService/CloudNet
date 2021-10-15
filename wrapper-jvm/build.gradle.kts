/*
 * Copyright 2019-2021 CloudNetService team & contributors
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
  id("com.github.johnrengelman.shadow") version "7.1.0"
}

tasks.withType<ShadowJar>() {
  archiveFileName.set(Files.wrapper)
  archiveVersion.set(null as String?)

  // netty relocation
  relocate("io.netty", "eu.cloudnetservice.relocate.netty")
  relocate("META-INF/native/netty", "META-INF/native/eu_cloudnetservice_relocate_netty")
  relocate("META-INF/native/libnetty", "META-INF/native/eu_cloudnetservice_relocate_libnetty")

  // google lib relocation
  relocate("com.google.gson", "eu.cloudnetservice.relocate.gson")
  relocate("com.google.common", "eu.cloudnetservice.relocate.guava")

  // drop unused classes which are making the jar bigger
  minimize()

  doFirst {
    from(exportLanguageFileInformation())
  }
}

dependencies {
  "api"(project(":cloudnet-driver"))
  "compileOnly"(project(":cloudnet-wrapper-jvm:minecraft-launchwrapper-api"))
}

applyJarMetadata("de.dytanic.cloudnet.wrapper.Main", "eu.cloudnetservice.wrapper")
