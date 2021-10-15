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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("com.github.johnrengelman.shadow") version "7.1.0"
}

tasks.withType<ShadowJar>() {
  dependsOn(":cloudnet-common:jar")

  archiveFileName.set(Files.driver)
  dependencies {
    exclude {
      it.moduleGroup != "eu.cloudnetservice.cloudnet"
    }
  }
}

tasks.withType<Test>() {
  dependsOn(":cloudnet-common:jar")
}

dependencies {
  "api"(project(":cloudnet-common"))
  "testImplementation"(project(":cloudnet-common").sourceSets().main.get().output)

  "implementation"("org.javassist", "javassist", Versions.javassist)
  "implementation"("io.netty", "netty-handler", Versions.netty)
  "implementation"("io.netty", "netty-codec-http", Versions.netty)
  "implementation"("io.netty", "netty-transport-native-epoll", Versions.netty, classifier = "linux-x86_64")

  "testImplementation"("org.glassfish.tyrus.bundles", "tyrus-standalone-client", Versions.tyrusClient)
}
