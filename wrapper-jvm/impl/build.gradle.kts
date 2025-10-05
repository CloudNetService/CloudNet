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

import eu.cloudnetservice.cloudnet.gradle.tasks.ExportCnlFile
import eu.cloudnetservice.cloudnet.gradle.tasks.ExportLanguageFileInformation
import eu.cloudnetservice.cloudnet.gradle.util.Files
import eu.cloudnetservice.cloudnet.gradle.util.applyJarMetadata

plugins {
  id("cloudnet-java")
  id("cloudnet-publish")
  alias(libs.plugins.shadow)
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

tasks.shadowJar.configure {
  archiveFileName = Files.wrapper
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  configurations = project.configurations.runtimeClasspath.map { setOf(it) }

  // do not shade dependencies which we don't need to shade
  dependencies {
    exclude {
      it.moduleGroup != rootProject.group && !ignoredGroupIds.contains(it.moduleGroup)
    }
  }

  // google lib relocation
  relocate("com.google.gson", "eu.cloudnetservice.relocate.gson")
  relocate("com.google.common", "eu.cloudnetservice.relocate.guava")

  // drop unused classes which are making the jar bigger
  minimize {
    exclude {
      it.moduleGroup == rootProject.group
    }
  }

  // exclude some config files that are pulled in by dependencies
  exclude("META-INF/LICENSE")
  exclude("META-INF/maven/**")
  exclude("META-INF/proguard/**")

  from(exportCnlFile)
  from(exportLanguageFileInformation)
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/wrapper.aero")
}

dependencies {
  api(projects.utils.utilsBase)
  api(projects.driver.driverApi)
  api(projects.driver.driverImpl)
  api(projects.wrapperJvm.wrapperJvmApi)

  // internal libraries
  implementation(libs.gson)
  implementation(libs.guava)
  implementation(libs.logbackCore)
  implementation(libs.logbackClassic)
  implementation(libs.bundles.aerogel)
  implementation(projects.ext.modlauncher)
  implementation(projects.ext.platformInjectSupport.platformInjectLoader)

  // processing
  annotationProcessor(libs.aerogelAuto)
  annotationProcessor(projects.driver.driverAp)
}

tasks.shadowJar.applyJarMetadata(
  indraGit,
  mainClass = "eu.cloudnetservice.wrapper.impl.Main",
  automaticModuleName = "eu.cloudnetservice.wrapper",
  preMain = "eu.cloudnetservice.wrapper.impl.Premain",
)
