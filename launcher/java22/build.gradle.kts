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

import eu.cloudnetservice.cloudnet.gradle.util.Files
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta.Data
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta.Type
import eu.cloudnetservice.cloudnet.gradle.util.applyJarMetadata

plugins {
  alias(libs.plugins.shadow)
  id("cloudnet-java")
  id("cloudnet-git")
  id("cloudnet-updater")
}

tasks.shadowJar.configure {
  archiveFileName.set(Files.launcher)
}

tasks.prepareUpdaterData {
  val archiveName = fromArchive(tasks.shadowJar)
  meta.set(archiveName.map { UpdaterMeta(Type.LAUNCHER, Data.Node(it)) })
}

dependencies {
  "implementation"(projects.ext.updater)
  "implementation"(projects.launcher.java8)
}

tasks.shadowJar.applyJarMetadata(git, "eu.cloudnetservice.launcher.java8.Launcher", "eu.cloudnetservice.launcher")
