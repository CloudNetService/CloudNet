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

import eu.cloudnetservice.cloudnet.gradle.Files
import eu.cloudnetservice.cloudnet.gradle.applyJarMetadata
import eu.cloudnetservice.cloudnet.gradle.plugins.updater.Data
import eu.cloudnetservice.cloudnet.gradle.plugins.updater.Meta
import eu.cloudnetservice.cloudnet.gradle.plugins.updater.Type

plugins {
  id("cloudnet-java")
  id("cloudnet-git")
  id("cloudnet-updater")
}

tasks.jar.configure {
  archiveFileName.set(Files.launcherPatcher)
}

tasks.prepareUpdaterData {
  val archiveName = fromArchive(tasks.jar)
  meta.set(archiveName.map { Meta(Type.LAUNCHER_PATCHER, Data.Node(it)) })
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = JavaVersion.VERSION_17.toString()
  targetCompatibility = JavaVersion.VERSION_17.toString()
}

git.applyJarMetadata(
  tasks.jar, "eu.cloudnetservice.launcher.patcher.CloudNetLauncherPatcher", "eu.cloudnetservice.launcher"
)
