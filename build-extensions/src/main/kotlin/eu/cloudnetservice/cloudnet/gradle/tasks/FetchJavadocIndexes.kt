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

package eu.cloudnetservice.cloudnet.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.nio.file.Files

private const val COMPONENT_ELEMENT_LIST = "element-list"
private const val COMPONENT_PACKAGE_LIST = "package-list"

@CacheableTask
abstract class FetchJavadocIndexes : DefaultTask() {
  @get:Input
  abstract val externalDocs: MapProperty<String, String> // baseUrl -> folder

  @get:OutputDirectory
  abstract val cacheDir: DirectoryProperty

  @TaskAction
  fun run() {
    val cache = cacheDir.get().asFile.also { it.mkdirs() }
    externalDocs.get().forEach { (baseUrl, folder) ->
      val target = cache.resolve(folder).also { it.mkdirs() }
      if (!tryFetch(target, baseUrl, COMPONENT_ELEMENT_LIST)) {
        if (!tryFetch(target, baseUrl, COMPONENT_PACKAGE_LIST)) {
          throw IllegalStateException("Unable to download javadoc indexes from $baseUrl")
        }
      }
    }
  }

  private fun tryFetch(target: File, baseUrl: String, component: String): Boolean {
    // skip download if the target directory already exists
    val dest = target.resolve(component)
    if (dest.exists()) {
      return true
    }

    return try {
      URI("$baseUrl$component").toURL().openStream().use { ins ->
        Files.newOutputStream(dest.toPath()).use { out -> ins.copyTo(out) }
      }
      dest.length() > 0
    } catch (_: Exception) {
      dest.delete()
      return false
    }
  }
}
