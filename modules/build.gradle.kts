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
plugins {
  id("eu.cloudnetservice.juppiter") version "0.1.2" apply false
}

subprojects {
  apply(plugin = "eu.cloudnetservice.juppiter")

  repositories {
    maven("https://jitpack.io/")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.nukkitx.com/maven-snapshots/")
    maven("https://repo.md-5.net/repository/releases/")
    maven("https://repo.waterdog.dev/artifactory/main/")
    maven("https://repo.velocitypowered.com/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
  }

  dependencies {
    "compileOnly"(project(":cloudnet-node"))
  }
}
