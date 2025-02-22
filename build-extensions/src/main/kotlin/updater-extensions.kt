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

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

fun generateUpdaterInformation() {
  UpdaterFiles.targetDirectory.deleteRecursively()
  UpdaterFiles.targetDirectory.mkdir()
  // module information
  updateModuleInformation()
  // node & launcher
  prepareNodeAndLauncherInfo()
}

@Throws(IOException::class)
private fun prepareNodeAndLauncherInfo() {
  // first copy the node and launcher to the target directory
  val nodeOut = UpdaterFiles.nodeFile.copyTo(UpdaterFiles.updaterOutputFile("node"), true)
  val launcherOut = UpdaterFiles.launcherFile.copyTo(UpdaterFiles.updaterOutputFile("launcher"), true)
  val launcherPatcherOut = UpdaterFiles.launcherPatcherFile.copyTo(UpdaterFiles.updaterOutputFile("launcher-patcher"), true)
  // generate the checksums for those files
  val nodeChecksum = ChecksumHelper.fileShaSum(nodeOut)
  val launcherChecksum = ChecksumHelper.fileShaSum(launcherOut)
  val launcherPatcherChecksum = ChecksumHelper.fileShaSum(launcherPatcherOut)
  val modulesJsonChecksum = ChecksumHelper.fileShaSum(UpdaterFiles.modulesJson)
  // set the checksums
  val checksums = Properties().apply {
    setProperty("node", nodeChecksum)
    setProperty("launcher", launcherChecksum)
    setProperty("modules-json", modulesJsonChecksum)
    setProperty("launcher-patcher", launcherPatcherChecksum)
  }
  // write the checksums
  FileWriter(UpdaterFiles.updaterOutputFile("checksums", "properties")).use {
    checksums.store(it, "Checksums for CloudNet ${Versions.cloudNet}-${Versions.cloudNetCodeName}")
  }
}

private fun updateModuleInformation() {
  // parse the current module file
  val moduleJsonFile = FileReader(File("modules.json")).use {
    JsonParser.parseReader(it).asJsonObject.get("entries").asJsonArray
  }
  // read all modules
  val internalModules = collectModuleInformation()
  // append the information of all internal modules to the object
  internalModules.forEach {
    moduleJsonFile.add(JsonObject().apply {
      addProperty("official", true)
      addProperty("name", it.second.get("name").asString)
      addProperty("website", it.second.get("website").asString)
      addProperty("version", it.second.get("version").asString)
      addProperty("sha3256", ChecksumHelper.fileShaSum(it.first))
      addProperty("description", it.second.get("description").asString)
      addProperty("url", "https://github.com/%updateRepo%/raw/%updateBranch%/modules/${it.first.name}")

      add("maintainers", stringJsonArray(it.second.get("author").asString))
      add("releaseNotes", stringJsonArray("Working with CloudNet ${Versions.cloudNet}"))

      add("dependingModules", JsonArray().apply {
        val dependencies = it.second.get("dependencies")?.asJsonArray ?: JsonArray()
        dependencies
          .map { it.asJsonObject }
          .filter { it.get("repo") == null }
          .map { it.get("name").asString }
          .forEach { add(it) }
      })
    })
  }
  // write the file
  FileWriter(UpdaterFiles.modulesJson).use {
    Gson().toJson(JsonObject().apply { add("entries", moduleJsonFile) }, it)
  }

  // copy all module files
  internalModules.map { it.first }.forEach {
    it.copyTo(UpdaterFiles.moduleTargetDirectory.resolve(it.name))
  }
}

private fun collectModuleInformation(): List<Pair<File, JsonObject>> {
  return UpdaterFiles.modulesDirectory.walkTopDown()
    .filter { it.name != "src" }
    .filter { it.name.startsWith("cloudnet-") && it.name.endsWith(".jar") }
    .map {
      // a module is located in build/libs - it's json is located in build/generated/module-json
      FileReader(it.parentFile.parentFile.resolve("generated/module-json/module.json")).use { reader ->
        Pair(it, JsonParser.parseReader(reader).asJsonObject)
      }
    }
    .toList()
}

private fun stringJsonArray(entry: String): JsonArray {
  return JsonArray(1).apply { add(entry) }
}

object UpdaterFiles {

  val modulesDirectory: File = File("modules")
  val targetDirectory: File = File(".launchermeta")

  val modulesJson: File = targetDirectory.resolve("modules.json")
  val moduleTargetDirectory: File = targetDirectory.resolve("modules")

  val nodeFile: File = gradleOutputJar("node/impl", "cloudnet")
  val launcherFile: File = gradleOutputJar("launcher/java22", "launcher")
  val launcherPatcherFile: File = gradleOutputJar("launcher/patcher", "launcher-patcher")

  fun updaterOutputFile(filename: String, fileExtension: String = "jar", subdirectory: String? = null): File {
    return targetDirectory.resolve("${subdirectory?.plus("/") ?: ""}$filename.$fileExtension")
  }

  private fun gradleOutputJar(projectName: String, outputFile: String): File {
    return File("$projectName/build/libs/$outputFile.jar")
  }
}

// Copies from the updater to keep consistent when generating checksums
object ChecksumHelper {

  @Throws(IOException::class)
  fun fileShaSum(path: File): String {
    return newSha3256Digest().run {
      update(path.readBytes())
      bytesToHex(digest())
    }
  }

  @Throws(NoSuchAlgorithmException::class)
  private fun newSha3256Digest(): MessageDigest = MessageDigest.getInstance("SHA3-256")

  private fun bytesToHex(input: ByteArray): String {
    val buffer = StringBuilder()
    for (b in input) {
      buffer.append(Character.forDigit(b.toInt() shr 4 and 0xF, 16))
      buffer.append(Character.forDigit(b.toInt() and 0xF, 16))
    }
    // convert to a string
    return buffer.toString()
  }
}
