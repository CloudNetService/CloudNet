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

tasks.withType<Jar> {
  dependsOn(":cloudnet-wrapper-jvm:shadowJar")

  archiveFileName.set(Files.node)

  from("../wrapper-jvm/build/libs") {
    include(Files.wrapper)
  }

  doFirst {
    exportCnlFile(Files.nodeCnl)
    from(exportLanguageFileInformation())
  }
}

dependencies {
  "api"(project(":cloudnet-driver"))
  // console
  "api"("org.jline", "jline", Versions.jline)
  "api"("org.fusesource.jansi", "jansi", Versions.jansi)
  // default database implementations
  "api"("com.h2database", "h2", Versions.h2)
  "api"("org.jetbrains.xodus", "xodus-environment", Versions.xodus)
  // jwt for rest api
  "api"("io.jsonwebtoken", "jjwt-api", Versions.jjwt)
  "api"("io.jsonwebtoken", "jjwt-impl", Versions.jjwt)
  "api"("io.jsonwebtoken", "jjwt-gson", Versions.jjwt)
  // commands
  "api"("cloud.commandframework", "cloud-core", Versions.cloud)
  "api"("cloud.commandframework", "cloud-annotations", Versions.cloud)
  // just for slf4j to disable the warning message
  "api"("org.slf4j", "slf4j-nop", Versions.slf4j)
}

applyJarMetadata("de.dytanic.cloudnet.BootLogic", "eu.cloudnetservice.cloudnet")
