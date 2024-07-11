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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  alias(libs.plugins.shadow)
}

tasks.withType<ShadowJar> {
  archiveFileName.set(Files.wrapper)
  archiveVersion.set(null as String?)

  // do not shade dependencies which we don't need to shade
  val ignoredGroupIds = arrayOf("com.google.guava", "com.google.code.gson")
  dependencies {
    exclude {
      it.moduleGroup != rootProject.group && !ignoredGroupIds.contains(it.moduleGroup)
    }
  }

  // google lib relocation
  relocate("com.google.gson", "eu.cloudnetservice.relocate.gson")
  relocate("com.google.common", "eu.cloudnetservice.relocate.guava")

  // drop unused classes which are making the jar bigger
  minimize()

  doFirst {
    // Note: included dependencies will not be resolved, they must be available from the node resolution already
    from(exportLanguageFileInformation())
    from(exportCnlFile("wrapper.cnl", ignoredGroupIds))
  }
}

tasks.withType<JavaCompile> {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/wrapper.aero")
}

dependencies {
  "api"(projects.driver)
  "api"(projects.ext.modlauncher)
  "api"(projects.ext.platformInjectSupport.platformInjectLoader)

  // internal libraries
  "implementation"(libs.gson)
  "implementation"(libs.guava)

  // processing
  "annotationProcessor"(libs.aerogelAuto)
}

applyJarMetadata(
  "eu.cloudnetservice.wrapper.Main",
  "eu.cloudnetservice.wrapper",
  "eu.cloudnetservice.wrapper.Premain")
