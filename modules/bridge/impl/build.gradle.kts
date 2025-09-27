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
  alias(libs.plugins.shadow)
  id("cloudnet-modules")
}

val shaded = configurations.register("shaded")
configurations.compileOnlyApi { extendsFrom(shaded.get()) }

tasks.withType<JavaCompile>().configureEach {
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
abstract class DownloadVersionedFabricMods : DefaultTask() {
  init {
    outputs.upToDateWhen { false } // permanent download url, cannot be cached
  }

  @get:Input
  abstract val downloadUrl: Property<String>

  @get:OutputFile
  abstract val nestedZipFile: RegularFileProperty

  @TaskAction
  fun run() {
    val out = nestedZipFile.get().asFile
    out.parentFile.mkdirs()
    URI(downloadUrl.get()).toURL().openStream().use { input ->
      NioFiles.copy(input, out.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
  }
}

val downloadVersionedFabricMods by tasks.registering(DownloadVersionedFabricMods::class) {
  val zipFileName = "cloudnet_fabric_version_bridge_all.zip"
  downloadUrl = "https://github.com/CloudNetService/cloudnet-bridge-fabric/releases/latest/download/$zipFileName"
  nestedZipFile = layout.buildDirectory.file("download/$zipFileName")
}

abstract class UnpackVersionedFabricMods : Sync() {
  @get:Inject
  abstract val archiveOps: ArchiveOperations
}

// unpacks the previously downloaded versioned mods zip into the target directory
val unpackVersionedFabricMods by tasks.registering(UnpackVersionedFabricMods::class) {
  from(downloadVersionedFabricMods.flatMap { it.nestedZipFile }.map { archiveOps.zipTree(it) })
  into(layout.buildDirectory.dir("generated/fabric_mods_nested"))
}

abstract class AddNestedJarsToFabricModJson : DefaultTask() {
  @get:InputFile
  abstract val modJson: RegularFileProperty

  @get:InputDirectory
  abstract val unpackDir: DirectoryProperty

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun run() {
    val apOutputJsonFile = modJson.get().asFile
    val jsonData = JsonSlurper().parse(apOutputJsonFile) as Map<*, *>
    val nestedJars = unpackDir.get().asFile
      .listFiles { file -> file.name.endsWith(".jar") }
      ?.map { file -> file.name }
      ?.map { fileName -> mapOf("file" to "bridge_mods_nested/$fileName") }
      ?: listOf()
    val mergedJson = HashMap(jsonData).apply {
      this["jars"] = nestedJars
    }

    val modJsonWithNestedJarsFile = outputFile.get().asFile
    modJsonWithNestedJarsFile.parentFile.mkdirs()
    modJsonWithNestedJarsFile.writeText(JsonOutput.toJson(mergedJson), StandardCharsets.UTF_8)
  }
}

val addNestedJarsToFabricModJson by tasks.registering(AddNestedJarsToFabricModJson::class) {
  outputFile = layout.buildDirectory.file("generated/fabric.mod.json")
  unpackDir = unpackVersionedFabricMods.map { it.destinationDir }
  modJson = tasks.compileJava.flatMap { it.destinationDirectory }.map { it.file("fabric.mod.json.temp") }
}

tasks.shadowJar.configure {
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
  dependsOn(addNestedJarsToFabricModJson)
  from(unpackVersionedFabricMods.map { it.destinationDir }) {
    into("bridge_mods_nested")
  }
  from(addNestedJarsToFabricModJson.map { it.outputFile }) {
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
