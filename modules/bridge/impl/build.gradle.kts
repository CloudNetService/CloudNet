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

import eu.cloudnetservice.cloudnet.gradle.tasks.IncludeNestedModJarsTask
import eu.cloudnetservice.cloudnet.gradle.util.Files

plugins {
  id("cloudnet-modules")
  id("cloudnet-publish")
  alias(libs.plugins.shadow)
}

val shaded = configurations.register("shaded")
configurations.named("compileOnlyApi") {
  extendsFrom(shaded.get())
}

repositories {
  maven("https://repo.waterdog.dev/releases/")
  maven("https://repo.waterdog.dev/snapshots/")
  maven("https://repo.loohpjames.com/repository")
  maven("https://repo.md-5.net/repository/releases/")
  maven("https://repo.md-5.net/repository/snapshots/")
  maven("https://repo.opencollab.dev/maven-releases/")
  maven("https://repo.opencollab.dev/maven-snapshots/")
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
  compileOnlyApi(projects.node.nodeImpl)
  compileOnlyApi(projects.utils.utilsBase)
  compileOnlyApi(projects.driver.driverImpl)
  compileOnlyApi(projects.wrapperJvm.wrapperJvmApi)
  compileOnly(projects.ext.platformInjectSupport.platformInjectApi)

  compileOnly(libs.guava)
  compileOnly(libs.reflexion)
  compileOnly(libs.fabricLoader)
  compileOnly(libs.bundles.proxyPlatform)
  compileOnly(libs.bundles.serverPlatform)

  shaded(projects.ext.adventureHelper)
  shaded(projects.ext.platformScheduler)
  shaded(projects.modules.bridge.bridgeApi)
  shaded(libs.bundles.adventure)
  shaded(libs.adventureSerializerBungee)

  annotationProcessor(libs.aerogelAuto)
  annotationProcessor(projects.driver.driverAp)
  annotationProcessor(projects.ext.platformInjectSupport.platformInjectProcessor)
}

tasks.withType<JavaCompile> {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/bridge.aero")
}

val zipFileName = "cloudnet_fabric_version_bridge_all.zip"
val downloadUrl = "https://github.com/CloudNetService/cloudnet-bridge-fabric/releases/latest/download/$zipFileName"
val includeNestedModJars = tasks.register<IncludeNestedModJarsTask>("includeNestedModJars") {
  dependsOn(tasks.compileJava)

  nestedZipDownloadUrl.set(downloadUrl)
  outputModJson.set(layout.buildDirectory.file("generated/fabric.mod.json"))
  nestedZipDownloadTarget.set(layout.buildDirectory.file("download/$zipFileName"))
  nestedModsDirectory.set(layout.buildDirectory.dir("generated/fabric_mods_nested"))
  baseModJson.set(layout.buildDirectory.file("classes/java/main/fabric.mod.json.temp"))
}

tasks.shadowJar {
  archiveFileName = Files.bridge
  configurations = setOf(project.configurations["shaded"])

  // pulled in by adventure but is present on the classpath anyway
  dependencies {
    exclude(dependency("com.google.code.gson:gson"))
  }

  // exclude our template plugin manifest template files from the final jar
  exclude("**/*.template")
  exclude("fabric.mod.json.temp")

  // depend on nested jar download, copy the nested jars into the final jar
  dependsOn(includeNestedModJars)
  from(includeNestedModJars.map { it.nestedModsDirectory }) {
    into("bridge_mods_nested")
  }
  from(includeNestedModJars.map { it.outputModJson })

  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  manifest {
    attributes["paperweight-mappings-namespace"] = "mojang"
  }
}

moduleJson {
  name = "CloudNet-Bridge"
  author = "CloudNetService"
  main = "eu.cloudnetservice.modules.bridge.impl.node.CloudNetBridgeModule"
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development"
}
