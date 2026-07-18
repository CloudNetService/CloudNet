/*
 * Copyright 2019-present CloudNetService team & contributors
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("INTERNAL_BUILD_SERVICE_USAGE")

pluginManagement {
  includeBuild("build-extensions")
  repositories {
    gradlePluginPortal()
    maven {
      name = "Paper-Snapshots"
      url = uri("https://repo.papermc.io/repository/maven-snapshots/")
    }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "cloudnet-parent"

registerSubProjects(
  root = "ext",
  subProjects = arrayOf("adventure-helper", "bukkit-command", "modlauncher", "updater", "platform-scheduler"),
)
registerSubProjects(
  root = "ext:platform-inject-support",
  prefix = "platform-inject",
  subProjects = arrayOf("api", "loader", "processor", "runtime"),
)
registerSubProjects(
  root = "utils",
  prefix = "utils",
  subProjects = arrayOf("base"),
)

registerSubProjects(
  root = "driver",
  prefix = "driver",
  subProjects = arrayOf("ap", "api", "impl"),
)
registerSubProjects(
  root = "wrapper-jvm",
  prefix = "wrapper-jvm",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "node",
  prefix = "node",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "launcher",
  prefix = "launcher",
  subProjects = arrayOf("java8", "java22", "patcher"),
)

registerSubProjects(
  root = "modules:bridge",
  prefix = "bridge",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:cloudflare",
  prefix = "cloudflare",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:database-mongodb",
  prefix = "database-mongodb",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:database-mysql",
  prefix = "database-mysql",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:dockerized-services",
  prefix = "dockerized-services",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:influx",
  prefix = "influx",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:labymod",
  prefix = "labymod",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:npcs",
  prefix = "npcs",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:report",
  prefix = "report",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:signs",
  prefix = "signs",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:smart",
  prefix = "smart",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:storage-s3",
  prefix = "storage-s3",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:storage-sftp",
  prefix = "storage-sftp",
  subProjects = arrayOf("api", "impl"),
)
registerSubProjects(
  root = "modules:syncproxy",
  prefix = "syncproxy",
  subProjects = arrayOf("api", "impl"),
)

include("bom")
include("plugins:luckperms")
include("plugins:papi-expansion")

//
private fun registerSubProjects(root: String, prefix: String? = null, vararg subProjects: String) {
  val subProjectNamePrefix = if (prefix.isNullOrBlank()) "" else "$prefix-"
  subProjects.forEach {
    val projectPath = "$root:$it"
    include(projectPath)
    project(":$projectPath").name = "$subProjectNamePrefix$it"
  }
}
