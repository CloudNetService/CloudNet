/*
 * Copyright 2019-2022 CloudNetService team & contributors
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
  dependsOn(":cloudnet-common:jar", ":cloudnet-common:javadocJar", ":cloudnet-common:sourcesJar")

  archiveFileName.set(Files.driver)
  dependencies {
    exclude {
      it.moduleGroup != "eu.cloudnetservice.cloudnet"
    }
  }
}

tasks.withType<Test> {
  dependsOn(":cloudnet-common:jar", ":cloudnet-common:javadocJar", ":cloudnet-common:sourcesJar")
}

extensions.configure<JavaPluginExtension> {
  sourceSets {
    create("ap")
  }
}

dependencies {
  "api"(projects.cloudnetCommon)
  "api"(projects.cloudnetExt.updater)

  "api"(libs.asm)
  "api"(libs.caffeine)
  "implementation"(libs.bundles.netty)

  // native transports
  "implementation"(variantOf(libs.nettyNativeKqueue) { classifier("osx-x86_64") })
  "implementation"(variantOf(libs.nettyNativeEpoll) { classifier("linux-x86_64") })
  "implementation"(variantOf(libs.nettyNativeIoUring) { classifier("linux-x86_64") })

  // hack - depend on the output of the ap output to apply the annotation process to this project too
  "annotationProcessor"(project.sourceSets()["ap"].output)

  "testImplementation"(libs.tyrus)
  "testImplementation"(projects.cloudnetCommon.dependencyProject.sourceSets()["main"].output)
}
