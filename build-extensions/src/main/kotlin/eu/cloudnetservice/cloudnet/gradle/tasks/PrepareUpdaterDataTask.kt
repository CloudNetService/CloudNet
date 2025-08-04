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

import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta.Data
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta.Type
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask

@CacheableTask
abstract class PrepareUpdaterDataTask : Sync() {
  @get:Input
  abstract val meta: Property<UpdaterMeta>

  init {
    meta.convention(project.provider { UpdaterMeta(Type.EMPTY, Data.Empty) })

    val metaResource = meta.map {
      project.resources.text.fromString(UpdaterMeta.Companion.gson.toJson(it))
    }

    from(metaResource) { rename { "meta.json" } }
  }

  fun fromArchive(task: Provider<out AbstractArchiveTask>, archiveName: Provider<String>? = null): Provider<String> {
    // We create an extra property to work around a gradle limitation
    // https://github.com/gradle/gradle/issues/16777#issuecomment-1424863032
    val property = project.objects.fileProperty()
    property.convention(task.flatMap { it.archiveFile })

    val archiveName = archiveName ?: property.locationOnly.map { it.asFile.name }
    from(property) { rename { archiveName.get() } }
    return archiveName
  }

  @TaskAction
  override fun copy() {
    val meta = meta.get()
    if (meta.type == Type.EMPTY) {
      logger.error("[{}] Found updater metadata with EMPTY type. Please configure this task.", path)
    }
    super.copy()
  }
}
