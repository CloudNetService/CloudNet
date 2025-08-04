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

import eu.cloudnetservice.cloudnet.gradle.tasks.ExportCnlFile
import eu.cloudnetservice.cloudnet.gradle.tasks.ExportLanguageFileInformation
import eu.cloudnetservice.cloudnet.gradle.util.Files
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta.Data
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta.Type
import eu.cloudnetservice.cloudnet.gradle.util.applyJarMetadata

plugins {
  alias(libs.plugins.shadow)
  id("cloudnet-java")
  id("cloudnet-git")
  id("cloudnet-updater")
}

val exportCnlFile = tasks.register<ExportCnlFile>("exportCnlFile") {
  fileName = Files.nodeCnl
  setResolvedArtifacts(configurations.runtimeClasspath)
}
val exportLanguageFileInformation = tasks.register<ExportLanguageFileInformation>("exportLanguageFileInformation") {
  languageFiles.from(project.projectDir.resolve("src/main/resources/lang").listFiles())
}
val includeInJar = configurations.register("includeInJar") { isTransitive = false }
val wrapperJar = configurations.register("wrapperJar") { isTransitive = false }

tasks.shadowJar {
  archiveFileName.set(Files.node)
  configurations = listOf(includeInJar.get())
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  from(wrapperJar) {
    // Rename the file to make sure 100% it is correctly named
    rename { Files.wrapper }
  }

  from(exportCnlFile)
  from(exportLanguageFileInformation)
}

tasks.prepareUpdaterData {
  val archiveName = fromArchive(tasks.shadowJar)
  meta.set(archiveName.map { UpdaterMeta(Type.NODE, Data.Node(it)) })
}

tasks.withType<JavaCompile>().configureEach {
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

  includeInJar(projects.node.nodeApi)
  includeInJar(projects.utils.utilsBase)
  includeInJar(projects.driver.driverApi)
  includeInJar(projects.driver.driverImpl)

  wrapperJar(projects.wrapperJvm.wrapperJvmImpl) { targetConfiguration = "shadow" }
}

tasks.jar.applyJarMetadata(git, "eu.cloudnetservice.node.impl.boot.Bootstrap", "eu.cloudnetservice.node")
