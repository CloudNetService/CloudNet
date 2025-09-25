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

package eu.cloudnetservice.cloudnet.gradle.util

import net.kyori.indra.git.IndraGitExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.the

fun TaskProvider<out Jar>.applyJarMetadata(git: IndraGitExtension, mainClass: String, automaticModuleName: String) {
  applyJarMetadata(git, mainClass, automaticModuleName, null)
}

fun TaskProvider<out Jar>.applyJarMetadata(
  git: IndraGitExtension,
  mainClass: String,
  automaticModuleName: String,
  preMain: String?
) {
  this.configure {
    val projectVersion = project.version.toString()
    inputs.property("projectVersion", projectVersion)

    manifest.attributes(
      "Main-Class" to mainClass,
      "Automatic-Module-Name" to automaticModuleName,
      "Implementation-Vendor" to "CloudNetService",
      "Implementation-Title" to Versions.CLOUDNET_CODE_NAME,
      "Implementation-Version" to Versions.CLOUDNET,
    )

    // apply the pre-main class if given
    preMain?.let {
      manifest.attributes("Premain-Class" to it)
    }

    // this executes git and must therefore run at execution time,
    // not at configuration time (execution is not supported at that time)
    doFirst {
      git.applyVcsInformationToManifest(manifest)
    }
  }
}

fun Project.sourceSets(): SourceSetContainer = the<JavaPluginExtension>().sourceSets

fun MavenArtifactRepository.releasesOnly() = apply { mavenContent { releasesOnly() } }
fun MavenArtifactRepository.snapshotsOnly() = apply { mavenContent { snapshotsOnly() } }
