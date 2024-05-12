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

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  includeBuild("build-extensions")
  repositories {
    gradlePluginPortal()
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net/")
    }
  }
}

rootProject.name = "cloudnet-root"

// top level projects
include("bom", "ext", "common", "driver", "node", "wrapper-jvm", "launcher", "modules", "plugins")

// external lib helpers
initializeSubProjects("ext",
  "modlauncher",
  "adventure-helper",
  "bukkit-command",
  "updater",
  "platform-inject-support")
// inject support
initializePrefixedSubProjects(
  "ext:platform-inject-support",
  "platform-inject",
  "api", "loader", "processor", "runtime")
// plugins
initializeSubProjects("plugins", "papi-expansion")
// modules
initializeSubProjects("modules",
  "bridge",
  "report",
  "cloudflare",
  "rest",
  "database-mongodb",
  "database-mysql",
  "signs",
  "storage-sftp",
  "syncproxy",
  "smart",
  "labymod",
  "npcs",
  "storage-s3",
  "dockerized-services",
  "influx")
// launcher
initializeSubProjects("launcher", "java8", "java17", "patcher")

fun initializeSubProjects(rootProject: String, vararg names: String) {
  names.forEach {
    include("$rootProject:$it")
    // update the project properties
    project(":$rootProject:$it").name = it
    project(":$rootProject:$it").projectDir = file(rootProject).resolve(it)
  }
}

fun initializePrefixedSubProjects(rootProject: String, prefix: String, vararg names: String) {
  names.forEach {
    include("$rootProject:$it")
    project(":$rootProject:$it").name = "$prefix-$it"
  }
}
