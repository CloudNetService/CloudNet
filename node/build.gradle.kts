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

tasks.withType<Jar> {
  dependsOn(":wrapper-jvm:shadowJar")

  archiveFileName.set(Files.node)

  from("../wrapper-jvm/build/libs") {
    include(Files.wrapper)
  }

  doFirst {
    from(exportCnlFile(Files.nodeCnl))
    from(exportLanguageFileInformation())
  }

  from(projects.driver.sourceSets()["main"].output)
  from(projects.common.sourceSets()["main"].output)
}

dependencies {
  "api"(projects.driver)
  "api"(projects.ext.updater)

  // dependencies which are available for modules
  "api"(libs.guava)
  "api"(libs.javers)
  "api"(libs.bundles.cloud)

  // internal libraries
  "implementation"(libs.h2)
  "implementation"(libs.asm)
  "implementation"(libs.gson)
  "implementation"(libs.xodus)
  "implementation"(libs.jline)
  "implementation"(libs.jansi)
  "implementation"(libs.slf4jNop)
  "implementation"(libs.bundles.jjwt)
  "implementation"(libs.bundles.nightConfig)
}

applyJarMetadata("eu.cloudnetservice.node.BootLogic", "eu.cloudnetservice.node")
