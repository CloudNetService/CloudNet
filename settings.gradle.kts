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

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  includeBuild("build-extensions")
}

rootProject.name = "cloudnet-root"

// top level projects
initializeProjects("ext", "common", "driver", "node", "wrapper-jvm", "launcher", "modules", "plugins")

// external lib helpers
initializeSubProjects("ext", "modlauncher", "adventure-helper", "bukkit-command")
// plugins
initializeSubProjects("plugins", "chat", "simplenametags")
// modules
initializeSubProjects("modules",
  "bridge",
  "report",
  "cloudflare",
  "rest",
  "database-mongodb",
  "database-mysql",
  "cloudperms",
  "signs",
  "storage-sftp",
  "syncproxy",
  "smart",
  "npcs")

fun initializeProjects(vararg names: String) {
  names.forEach {
    include("cloudnet-$it")
    project(":cloudnet-$it").projectDir = file(it)
  }
}

fun initializeSubProjects(rootProject: String, vararg names: String) {
  names.forEach {
    include("cloudnet-$rootProject:$it")
    // update the project properties
    project(":cloudnet-$rootProject:$it").name = it
    project(":cloudnet-$rootProject:$it").projectDir = file(rootProject).resolve(it)
  }
}
