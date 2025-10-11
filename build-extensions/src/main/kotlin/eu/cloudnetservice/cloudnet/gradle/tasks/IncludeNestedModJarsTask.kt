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

package eu.cloudnetservice.cloudnet.gradle.tasks

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

@UntrackedTask(because = "Downloads from a 'latest' URL; content can change without input change")
abstract class IncludeNestedModJarsTask : DefaultTask() {
  companion object {
    val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
  }

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val baseModJson: RegularFileProperty

  @get:Input
  abstract val nestedZipDownloadUrl: Property<String>

  @get:OutputFile
  abstract val nestedZipDownloadTarget: RegularFileProperty

  @get:OutputFile
  abstract val outputModJson: RegularFileProperty

  @get:OutputDirectory
  abstract val nestedModsDirectory: DirectoryProperty

  @TaskAction
  fun run() {
    // download zip file containing the nested jar files
    val zipFile = nestedZipDownloadTarget.get().asFile
    zipFile.parentFile.mkdirs()
    URI(nestedZipDownloadUrl.get()).toURL().openStream().use {
      Files.copy(it, zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    // unpacks the previously downloaded versioned mods zip into the target directory
    // note: the zip unpack process does not support directories on purpose, all jars should be on the root-level
    val targetDir = nestedModsDirectory.get().asFile
    targetDir.deleteRecursively()
    targetDir.mkdirs()
    ZipFile(zipFile).use {
      it.entries().asSequence().forEach { entry ->
        it.getInputStream(entry).use { inputStream ->
          val out = targetDir.resolve(entry.name)
          Files.copy(inputStream, out.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
      }
    }

    // update the fabric.mod.json input file to append the downloaded nested jars
    val baseModJsonFile = baseModJson.get().asFile
    val parsedJson = JsonParser.parseString(baseModJsonFile.readText()).asJsonObject
    val nestedJarPaths = targetDir
      .listFiles { f -> f.isFile && f.name.endsWith(".jar") }
      ?.sortedBy { it.name }
      ?.map {
        JsonObject().apply {
          addProperty("file", "bridge_mods_nested/${it.name}")
        }
      }
      ?: emptyList()
    val nestedJars = parsedJson.getAsJsonArray("jars") ?: JsonArray()
    nestedJars.apply { nestedJarPaths.forEach { add(it) } }
    parsedJson.add("jars", nestedJars)

    val outModJsonFile = outputModJson.get().asFile
    outModJsonFile.parentFile.mkdirs()
    outModJsonFile.writeText(gson.toJson(parsedJson))
  }
}
