/*
 * Copyright 2019-2025 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.gradle.plugins.updater

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import eu.cloudnetservice.cloudnet.gradle.ChecksumHelper
import eu.cloudnetservice.cloudnet.gradle.Versions
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.util.*

abstract class GenerateUpdaterInformationTask : DefaultTask() {

  @get:InputFiles
  abstract val artifacts: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val destinationDirectory: DirectoryProperty

  @get:InputFile
  abstract val initialModules: RegularFileProperty

  init {
    initialModules.convention(project.layout.file(project.provider { project.file("modules.json") }))
    dependsOn(artifacts)
  }

  @TaskAction
  fun run() {
    val destination = destinationDirectory.get().asFile
    destination.deleteRecursively()
    destination.mkdirs()
    val modulesDirectory = destination.resolve("modules")
    modulesDirectory.mkdirs()

    val moduleJsonArray = initialModules.get().asFile.bufferedReader().use {
      JsonParser.parseReader(it).asJsonObject.get("entries").asJsonArray
    }

    val checksums = Properties()

    artifacts.forEach { directory ->
      val meta = directory.resolve("meta.json").readText().run { Meta.gson.fromJson(this, Meta::class.java) }
      val data = meta.data
      when (meta.type) {
        Type.MODULE -> {
          data as Data.Module
          val moduleJson = directory.resolve(data.moduleJsonName).bufferedReader().use {
            JsonParser.parseReader(it).asJsonObject
          }
          val moduleFile = directory.resolve(data.archiveName)
          moduleJsonArray.add(JsonObject().apply {
            addProperty("official", true)
            addProperty("name", moduleJson.get("name").asString)
            addProperty("website", moduleJson.get("website").asString)
            addProperty("version", moduleJson.get("version").asString)
            addProperty("sha3256", ChecksumHelper.fileShaSum(moduleFile))
            addProperty("description", moduleJson.get("description").asString)

            add("maintainers", JsonArray(1).apply { add(moduleJson.get("author").asString) })
            add("releaseNotes", JsonArray(1).apply { add("Working with CloudNet ${Versions.cloudNet}") })

            add("dependingModules", JsonArray().apply {
              val dependencies = moduleJson.get("dependencies")?.asJsonArray ?: JsonArray()
              dependencies.map { it.asJsonObject }.filter { !it.has("repo") }.map { it.get("name").asString }
                .forEach { add(it) }
            })
          })
          moduleFile.copyTo(modulesDirectory.resolve(moduleFile.name))
        }

        Type.NODE -> {
          data as Data.Node
          val nodeFile = directory.resolve(data.archiveName)
          nodeFile.copyTo(destination.resolve("node.jar"))
          checksums.setProperty("node", ChecksumHelper.fileShaSum(nodeFile))
        }

        Type.LAUNCHER -> {
          data as Data.Launcher
          val launcherFile = directory.resolve(data.archiveName)
          launcherFile.copyTo(destination.resolve("launcher.jar"))
          checksums.setProperty("launcher", ChecksumHelper.fileShaSum(launcherFile))
        }

        Type.LAUNCHER_PATCHER -> {
          data as Data.LauncherPatcher
          val launcherPatcherFile = directory.resolve(data.archiveName)
          launcherPatcherFile.copyTo(destination.resolve("launcher-patcher.jar"))
          checksums.setProperty("launcher-patcher", ChecksumHelper.fileShaSum(launcherPatcherFile))
        }

        Type.EMPTY -> {
          logger.error("Detected empty metadata dependency. Please fix your configuration!")
        }
      }
    }

    val modulesJsonFile = destination.resolve("modules.json")
    modulesJsonFile.writeText(Gson().toJson(JsonObject().apply {
      add("entries", moduleJsonArray)
    }))
    checksums.setProperty("modules-json", ChecksumHelper.fileShaSum(modulesJsonFile))

    destination.resolve("checksums.properties").bufferedWriter().use {
      checksums.store(it, "Checksums for CloudNet ${Versions.cloudNet}-${Versions.cloudNetCodeName}")
    }
  }
}
