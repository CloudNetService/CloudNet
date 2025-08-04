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

package eu.cloudnetservice.cloudnet.gradle

import eu.cloudnetservice.cloudnet.gradle.plugins.git.GitExtension
import eu.cloudnetservice.cloudnet.gradle.plugins.git.GitService
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
import java.util.*
import java.util.function.Function

fun GitExtension.applyJarMetadata(task: TaskProvider<out Jar>, mainClass: String, module: String) {
  applyJarMetadata(task, mainClass, module, null)
}

fun GitExtension.applyJarMetadata(task: TaskProvider<out Jar>, mainClass: String, module: String, preMain: String?) {
  project.run {
    task.configure {
      val service = git()?.service
      val serviceOrEmpty = (service?.map { Optional.of(it) }) ?: provider { Optional.empty<GitService>() }
      service?.let { usesService(it) }

      val projectVersion = project.version.toString()

      manifest.attributes(
        "Main-Class" to mainClass,
        "Automatic-Module-Name" to module,
        "Implementation-Vendor" to "CloudNetService",
        "Implementation-Title" to Versions.cloudNetCodeName,
        "Implementation-Version" to serviceOrEmpty.shortCommitHash().map { "$projectVersion-$it" },
        "Git-Commit" to serviceOrEmpty.shortCommitHash(),
        "Git-Branch" to serviceOrEmpty.map("unknown") { it.branchName }
      )
      // apply the pre-main class if given
      preMain?.let {
        manifest.attributes("Premain-Class" to it)
      }
    }
  }
}

fun Provider<Optional<GitService>>.map(empty: String, function: Function<GitService, String?>) =
  map { o -> o.map { function.apply(it) }.orElse(empty) }

fun Provider<Optional<GitService>>.shortCommitHash(): Provider<String> {
  return map("unknown") { it.commit?.name?.substring(0, 8) }
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
