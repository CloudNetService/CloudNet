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
  dependsOn(":cloudnet-wrapper-jvm:shadowJar")

  archiveFileName.set(Files.node)

  from("../wrapper-jvm/build/libs") {
    include(Files.wrapper)
  }

  doFirst {
    from(exportCnlFile(Files.nodeCnl))
    from(exportLanguageFileInformation())
  }

  from(projects.cloudnetDriver.sourceSets()["main"].output)
  from(projects.cloudnetCommon.sourceSets()["main"].output)
}

tasks.withType<Test> {
  dependsOn(":cloudnet-common:jar", ":cloudnet-common:javadocJar", ":cloudnet-common:sourcesJar")
}

dependencies {
  "api"(projects.cloudnetDriver)
  "api"(projects.cloudnetExt.updater)
  // console
  "api"(libs.jline)
  "api"(libs.jansi)
  // javers - diff finder for java objects (used for cluster data sync)
  "api"(libs.javers)
  // default database implementations
  "api"(libs.h2)
  "api"(libs.xodus)
  // just for slf4j to disable the warning message
  "api"(libs.slf4jNop)
  // jwt for rest api
  "api"(libs.bundles.jjwt)
  // commands
  "api"(libs.bundles.cloud)
  // nobody understands why gradle needs this
  "testImplementation"(projects.cloudnetCommon.dependencyProject.sourceSets()["main"].output)
}

applyJarMetadata("eu.cloudnetservice.cloudnet.node.BootLogic", "eu.cloudnetservice.cloudnet")
