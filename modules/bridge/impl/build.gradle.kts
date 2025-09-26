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

import eu.cloudnetservice.cloudnet.gradle.util.Files
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.StandardCopyOption
import java.nio.file.Files as NioFiles

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

// Downloads the versioned fabric mods zip from CloudNetService/cloudnet-bridge-fabric
// These are unpacked and transferred into the final jar in a subsequent task
val zipFileName = "cloudnet_fabric_version_bridge_all.zip"
val nestedZipFile = layout.buildDirectory.file("download/$zipFileName")
val downloadVersionedFabricMods by tasks.registering {
  outputs.upToDateWhen { false } // permanent download url, cannot be cached
  outputs.file(nestedZipFile)
  val downloadUrl = "https://github.com/CloudNetService/cloudnet-bridge-fabric/releases/latest/download/$zipFileName"
  doLast {
    val out = nestedZipFile.get().asFile
    out.parentFile.mkdirs()
    URI(downloadUrl).toURL().openStream().use { input ->
      NioFiles.copy(input, out.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
  }
}

// unpacks the previously downloaded versioned mods zip into the target directory
val nestedUnpackDir = layout.buildDirectory.dir("generated/fabric_mods_nested")
val unpackVersionedFabricMods by tasks.registering(Copy::class) {
  inputs.file(nestedZipFile)
  outputs.dir(nestedUnpackDir)
  from(zipTree(nestedZipFile))
  into(nestedUnpackDir)
}

// updates the fabric.mod.json file to include the nested jar paths
val apOutputModJson = layout.buildDirectory.file("classes/java/main/fabric.mod.json.temp")
val modJsonWithNestedJars = layout.buildDirectory.file("generated/fabric.mod.json")
val addNestedJarsToFabricModJson by tasks.registering {
  inputs.file(apOutputModJson)
  inputs.dir(nestedUnpackDir)
  outputs.file(modJsonWithNestedJars)

  doLast {
    val apOutputJsonFile = apOutputModJson.get().asFile
    val jsonData = JsonSlurper().parse(apOutputJsonFile) as Map<*, *>
    val nestedJars = nestedUnpackDir.get().asFile
      .listFiles { file -> file.name.endsWith(".jar") }
      ?.map { file -> file.name }
      ?.map { fileName -> mapOf("file" to "bridge_mods_nested/$fileName") }
      ?: listOf()
    val mergedJson = HashMap(jsonData).apply {
      this["jars"] = nestedJars
    }

    val modJsonWithNestedJarsFile = modJsonWithNestedJars.get().asFile
    modJsonWithNestedJarsFile.parentFile.mkdirs()
    modJsonWithNestedJarsFile.writeText(JsonOutput.toJson(mergedJson), StandardCharsets.UTF_8)
  }
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
  from(nestedUnpackDir) {
    into("bridge_mods_nested")
  }
  from(modJsonWithNestedJars) {
    into("")
  }

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
