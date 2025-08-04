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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.StandardCopyOption
import java.nio.file.Files as NioFiles
import eu.cloudnetservice.cloudnet.gradle.util.Files

plugins {
  id("cloudnet-modules")
}

configurations {
  val shaded = register("shaded")
  getByName("compileOnlyApi").extendsFrom(shaded.get())
}

tasks.withType<JavaCompile> {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/bridge.aero")
}

dependencies {
  "api"(projects.modules.bridge.bridgeApi)

  "compileOnly"(libs.reflexion)
  "compileOnly"(projects.node.nodeImpl)
  "compileOnly"(projects.utils.utilsBase)
  "compileOnly"(projects.driver.driverImpl)
  "compileOnly"(libs.fabricLoader)
  "compileOnly"(projects.wrapperJvm.wrapperJvmApi)
  "compileOnly"(libs.bundles.proxyPlatform)
  "compileOnly"(libs.bundles.serverPlatform)

  "shaded"(projects.ext.adventureHelper)
  "shaded"(projects.modules.bridge.bridgeApi)
  "shaded"(libs.bundles.adventure)
  "shaded"(libs.adventureSerializerBungee)

  // processing
  "annotationProcessor"(libs.aerogelAuto)
  "annotationProcessor"(projects.driver.driverAp)
}

// Downloads the versioned fabric mods zip from CloudNetService/cloudnet-bridge-fabric
// These are unpacked and transferred into the final jar in a subsequent task
val zipFileName = "cloudnet_fabric_version_bridge_all.zip"
val nestedZipFile = layout.buildDirectory.file("download/$zipFileName")
val downloadVersionedFabricMods by tasks.registering {
  outputs.upToDateWhen { false } // permanent download url, cannot be cached
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
  dependsOn(downloadVersionedFabricMods)
  from(zipTree(nestedZipFile))
  into(nestedUnpackDir)
}

//
val apOutputModJson = layout.buildDirectory.file("classes/java/main/fabric.mod.json.temp")
val modJsonWithNestedJars = layout.buildDirectory.file("generated/fabric.mod.json")
val addNestedJarsToFabricModJson by tasks.registering {
  dependsOn(tasks.getByName("compileJava"), unpackVersionedFabricMods)

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
  dependsOn(unpackVersionedFabricMods, addNestedJarsToFabricModJson)
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
