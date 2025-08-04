/*
 * Copyright 2019-2025 CloudNetService team & contributors
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
import eu.cloudnetservice.cloudnet.gradle.applyJarMetadata
import eu.cloudnetservice.cloudnet.gradle.tasks.ExportCnlFile
import eu.cloudnetservice.cloudnet.gradle.tasks.ExportLanguageFileInformation
import eu.cloudnetservice.cloudnet.gradle.util.Files

plugins {
  alias(libs.plugins.shadow)
  id("cloudnet-java")
  id("cloudnet-git")
}

val ignoredGroupIds = listOf("com.google.guava", "com.google.code.gson")
val exportCnlFile = tasks.register<ExportCnlFile>("exportCnlFile") {
  fileName = "wrapper.cnl"
  ignoredDependencyGroups = ignoredGroupIds
  setResolvedArtifacts(configurations.runtimeClasspath)
}
val exportLanguageFileInformation = tasks.register<ExportLanguageFileInformation>("exportLanguageFileInformation") {
  languageFiles.from(project.projectDir.resolve("src/main/resources/lang").listFiles())
}

// intermediate task to take advantage of build cache when checking out another branch/commiting
// The git information is only included in the "shadowJar" task, so this won't have to rerun
val intermediateShadowJar = tasks.register<ShadowJar>("intermediateShadowJar") {
  this.configurations.set(project.configurations.runtimeClasspath.map { setOf(it) })

  // do not shade dependencies which we don't need to shade

  dependencies {
    exclude {
      it.moduleGroup != rootProject.group && !ignoredGroupIds.contains(it.moduleGroup)
    }
  }
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  // google lib relocation
  relocate("com.google.gson", "eu.cloudnetservice.relocate.gson")
  relocate("com.google.common", "eu.cloudnetservice.relocate.guava")

  // drop unused classes which are making the jar bigger
  minimize()

  from(exportLanguageFileInformation)
  from(exportCnlFile)

  destinationDirectory = temporaryDir
}

tasks.shadowJar.configure {
  archiveFileName.set(Files.wrapper)

  configurations.empty()
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  dependsOn(intermediateShadowJar)
  from(intermediateShadowJar.map { zipTree(it.archiveFile) })
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/wrapper.aero")
}

dependencies {
  "api"(projects.ext.modlauncher)
  "api"(projects.driver.driverApi)
  "api"(projects.driver.driverImpl)
  "api"(projects.wrapperJvm.wrapperJvmApi)
  "api"(projects.ext.platformInjectSupport.platformInjectLoader)

  // internal libraries
  "implementation"(libs.gson)
  "implementation"(libs.guava)
  "implementation"(libs.logbackCore)
  "implementation"(libs.logbackClassic)
  "implementation"(projects.utils.utilsBase)

  // processing
  "annotationProcessor"(libs.aerogelAuto)
  "annotationProcessor"(projects.driver.driverAp)
}

git.applyJarMetadata(
  tasks.jar,
  "eu.cloudnetservice.wrapper.impl.Main",
  "eu.cloudnetservice.wrapper",
  "eu.cloudnetservice.wrapper.impl.Premain")
