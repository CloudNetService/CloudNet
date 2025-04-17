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

import net.kyori.indra.git.IndraGitExtension
import net.kyori.indra.git.RepositoryValueSource
import net.kyori.indra.git.internal.IndraGitExtensionImpl
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import java.io.IOException
import java.net.HttpURLConnection
import java.util.logging.Level
import java.util.logging.Logger

fun Project.applyJarMetadata(mainClass: String, module: String) {
  applyJarMetadata(mainClass, module, null)
}

fun Project.applyJarMetadata(mainClass: String, module: String, preMain: String?) {
  if ("jar" in tasks.names) {
    tasks.named<Jar>("jar") {
      manifest.attributes(
        "Main-Class" to mainClass,
        "Automatic-Module-Name" to module,
        "Implementation-Vendor" to "CloudNetService",
        "Implementation-Title" to Versions.cloudNetCodeName,
        "Implementation-Version" to project.version.toString() + "-${shortCommitHash()}")
      // apply the pre-main class if given
      if (preMain != null) {
        manifest.attributes("Premain-Class" to preMain)
      }
      // apply git information to manifest
      git()?.let { git ->
        val commit = git.commit().map { it.name.substring(0, 8) }
        val branchName = git.repositoryValue(BranchName::class.java)
        if (commit.isPresent) manifest.attributes(IndraGitExtension.MANIFEST_ATTRIBUTE_GIT_COMMIT to commit.get())
        if (branchName.isPresent) manifest.attributes(IndraGitExtension.MANIFEST_ATTRIBUTE_GIT_BRANCH to branchName.get())
      }
    }
  }
}

/**
 * Indra does not properly support configuration caching for #branchName yet
 */
private abstract class BranchName : RepositoryValueSource.Parameterless<String>() {
  override fun obtain(repository: Git): String? {
    try {
      val ref = repository.repository.exactRef(Constants.HEAD)
      if (ref == null || !ref.isSymbolic) return null // no HEAD, or detached HEAD

      return ref.target.name
    } catch (ex: IOException) {
      Logger.getGlobal().log(Level.SEVERE, "Failed to query current branch name from git:", ex)
      return null
    }
  }
}

fun Project.shortCommitHash(): String {
  return git()?.commit()?.get()?.name()?.substring(0, 8) ?: "unknown"
}

fun Project.git(): IndraGitExtension? = rootProject.extensions.findByType()

fun Project.sourceSets(): SourceSetContainer = the<JavaPluginExtension>().sourceSets

fun Project.mavenRepositories(): Iterable<MavenArtifactRepository> = repositories.filterIsInstance<MavenArtifactRepository>()

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
