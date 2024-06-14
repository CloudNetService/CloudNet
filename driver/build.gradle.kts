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
  archiveFileName.set(Files.driver)
  dependencies {
    exclude {
      it.moduleGroup != "eu.cloudnetservice.cloudnet"
    }
  }
}

tasks.withType<JavaCompile> {
  options.compilerArgs = listOf("-AaerogelAutoFileName=autoconfigure/driver.aero")
}

tasks.withType<Test> {
  dependsOn(":common:jar")
}

dependencies {
  "api"(projects.common)
  "api"(projects.ext.updater)

  "api"(libs.caffeine)
  "api"(libs.reflexion)
  "api"(libs.geantyref)
  "api"(libs.bundles.unirest)
  "api"(libs.bundles.aerogel)

  // processing
  "annotationProcessor"(libs.aerogelAuto)

  // internal libraries
  "implementation"(libs.asm)
  "implementation"(libs.gson)
  "implementation"(libs.guava)

  // netty
  "implementation"(libs.bundles.netty)
  "implementation"(libs.nettyNativeKqueue)
  "implementation"(variantOf(libs.nettyNativeEpoll) { classifier("linux-x86_64") })
  "implementation"(variantOf(libs.nettyNativeEpoll) { classifier("linux-aarch_64") })

  "testImplementation"(projects.common.dependencyProject.sourceSets()["main"].output)
}
