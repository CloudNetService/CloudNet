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

import git.GitExtension
import git.GitService
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.the

fun GitExtension.applyJarMetadata(task: TaskProvider<out Jar>, mainClass: String, module: String) {
  applyJarMetadata(task, mainClass, module, null)
}

fun GitExtension.applyJarMetadata(task: TaskProvider<out Jar>, mainClass: String, module: String, preMain: String?) {
  project.run {
    task.configure {
      val service = git()?.service
      service?.let { usesService(it) }

      manifest.attributes(
        "Main-Class" to mainClass,
        "Automatic-Module-Name" to module,
        "Implementation-Vendor" to "CloudNetService",
        "Implementation-Title" to Versions.cloudNetCodeName,
        "Implementation-Version" to project.version.toString() + "-${service.shortCommitHash()}"
      )
      // apply the pre-main class if given
      if (preMain != null) {
        manifest.attributes("Premain-Class" to preMain)
      }
      // apply git information to manifest
      service?.run { get() }?.let { service ->
        service.commit?.name?.substring(0, 8)?.let { manifest.attributes("Git-Commit" to it) }
        service.branchName?.let { manifest.attributes("Git-Branch" to it) }
      }
    }
  }
}

fun Provider<GitService>?.shortCommitHash(): String {
  return this?.get()?.commit?.name()?.substring(0, 8) ?: "unknown"
}

fun Project.git(): GitExtension? = extensions.findByType()

fun Project.sourceSets(): SourceSetContainer = the<JavaPluginExtension>().sourceSets

fun releasesOnly(repository: MavenArtifactRepository) {
  repository.mavenContent {
    releasesOnly()
  }
}

fun snapshotsOnly(repository: MavenArtifactRepository) {
  repository.mavenContent {
    snapshotsOnly()
  }
}
