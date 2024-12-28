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
include("bom")

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
initializeSubProjects("plugins", "papi-expansion", "luckperms")
// modules
initializeSubProjects("modules",
  "bridge",
  "report",
  "cloudflare",
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
initializeSubProjects("launcher", "java8", "java22", "patcher")

// driver-api, driver-impl
initializePrefixedSubProjects("utils", "utils", "base")
initializePrefixedSubProjects("node", "node", "api", "impl")
initializePrefixedSubProjects("driver", "driver", "api", "impl", "ap")
initializePrefixedSubProjects("wrapper-jvm", "wrapper-jvm", "api", "impl")
initializePrefixedSubProjects("modules:npcs", "npcs", "api", "impl")
initializePrefixedSubProjects("modules:signs", "signs", "api", "impl")
initializePrefixedSubProjects("modules:smart", "smart", "api", "impl")
initializePrefixedSubProjects("modules:report", "report", "api", "impl")
initializePrefixedSubProjects("modules:bridge", "bridge", "api", "impl")
initializePrefixedSubProjects("modules:influx", "influx", "api", "impl")
initializePrefixedSubProjects("modules:labymod", "labymod", "api", "impl")
initializePrefixedSubProjects("modules:syncproxy", "syncproxy", "api", "impl")
initializePrefixedSubProjects("modules:cloudflare", "cloudflare", "api", "impl")
initializePrefixedSubProjects("modules:storage-s3", "storage-s3", "api", "impl")
initializePrefixedSubProjects("modules:storage-sftp", "storage-sftp", "api", "impl")
initializePrefixedSubProjects("modules:database-mysql", "database-mysql", "api", "impl")
initializePrefixedSubProjects("modules:database-mongodb", "database-mongodb", "api", "impl")
initializePrefixedSubProjects("modules:dockerized-services", "dockerized-services", "api", "impl")

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
