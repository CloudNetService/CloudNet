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

tasks.withType<Jar> {
  archiveFileName.set(Files.node)
  from(projects.node.nodeApi.sourceSets()["main"].output)
  from(projects.utils.utilsBase.sourceSets()["main"].output)
  from(projects.driver.driverApi.sourceSets()["main"].output)
  from(projects.driver.driverImpl.sourceSets()["main"].output)

  dependsOn(":wrapper-jvm:wrapper-jvm-impl:shadowJar")

  from("../../wrapper-jvm/impl/build/libs") {
    include(Files.wrapper)
  }

  doFirst {
    from(exportCnlFile(Files.nodeCnl))
    from(exportLanguageFileInformation())
  }
}

tasks.withType<JavaCompile> {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/node.aero")
}

dependencies {
  "api"(projects.driver.driverImpl)
  "api"(projects.node.nodeApi)
  "api"(projects.ext.updater)

  "implementation"(projects.utils.utilsBase)

  // dependencies which are available for modules
  "api"(libs.guava)
  "api"(libs.bundles.cloud) {
    exclude(group = "org.incendo", module = "cloud-core")
  }

  // processing
  "annotationProcessor"(libs.aerogelAuto)
  "annotationProcessor"(projects.driver.driverAp)

  // internal libraries

  "implementation"(libs.h2)
  "implementation"(libs.gson)
  "implementation"(libs.gulf)
  "implementation"(libs.xodus)
  "implementation"(libs.jansi)
  "implementation"(libs.caffeine)
  "implementation"(libs.bundles.jline)
  "implementation"(libs.bundles.cloud)
  "implementation"(libs.bundles.unirest)
  "implementation"(libs.stringSimilarity)
  "implementation"(libs.bundles.nightConfig)

  "implementation"(libs.logbackCore)
  "implementation"(libs.logbackClassic)

  "compileOnly"(libs.bundles.netty)
}

applyJarMetadata("eu.cloudnetservice.node.impl.boot.Bootstrap", "eu.cloudnetservice.node")
