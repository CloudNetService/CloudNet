/*
 * Copyright 2019-present CloudNetService team & contributors
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
  id("cloudnet-java")
  id("cloudnet-updater")
  id("cloudnet-publish")
  alias(libs.plugins.shadow)
}

val exportCnlFile = tasks.register<ExportCnlFile>("exportCnlFile") {
  fileName = Files.nodeCnl
  setResolvedArtifacts(configurations.runtimeClasspath)
}
val generateLanguageFileList = tasks.register<ExportLanguageFileInformation>("exportLanguageFileInformation") {
  languageFiles.from(project.projectDir.resolve("src/main/resources/lang").listFiles())
}

val shaded = configurations.register("shaded") { isTransitive = false }
val wrapperJar = configurations.register("wrapperJar") { isTransitive = false }

tasks.shadowJar {
  archiveFileName = Files.node
  configurations = listOf(shaded.get())
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

  from(wrapperJar.get()) {
    rename { Files.wrapper }
  }

  from(exportCnlFile)
  from(generateLanguageFileList)
}

tasks.prepareUpdaterData {
  val archiveName = fromArchive(tasks.shadowJar)
  meta.set(archiveName.map { UpdaterMeta(Type.NODE, Data.Node(it)) })
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/node.aero")
}

dependencies {
  api(projects.ext.updater)
  api(projects.node.nodeApi)
  api(projects.utils.utilsBase)
  api(projects.driver.driverImpl)

  // dependencies which are available for modules
  api(libs.guava)
  api(libs.bundles.cloud) {
    exclude(group = "org.incendo", module = "cloud-core")
  }

  // processing
  annotationProcessor(libs.aerogelAuto)
  annotationProcessor(projects.driver.driverAp)

  // internal libraries
  implementation(libs.h2)
  implementation(libs.gson)
  implementation(libs.gulf)
  implementation(libs.xodus)
  implementation(libs.jansi)
  implementation(libs.caffeine)
  implementation(libs.semver4j)
  implementation(libs.bundles.jline)
  implementation(libs.bundles.cloud)
  implementation(libs.bundles.unirest)
  implementation(libs.bundles.aerogel)
  implementation(libs.stringSimilarity)
  implementation(libs.bundles.nightConfig)

  implementation(libs.logbackCore)
  implementation(libs.logbackClassic)

  compileOnly(libs.bundles.netty)

  shaded(projects.node.nodeApi)
  shaded(projects.utils.utilsBase)
  shaded(projects.driver.driverApi)
  shaded(projects.driver.driverImpl)

  wrapperJar(projects.wrapperJvm.wrapperJvmImpl) { targetConfiguration = "shadow" }
}

tasks.jar.applyJarMetadata(
  indraGit,
  mainClass = "eu.cloudnetservice.node.impl.boot.Bootstrap",
  automaticModuleName = "eu.cloudnetservice.node",
)
