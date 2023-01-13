/*
 * Copyright 2019-2023 CloudNetService team & contributors
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
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import java.net.HttpURLConnection
import java.net.URL

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
      git()?.applyVcsInformationToManifest(manifest)
    }
  }
}

fun Project.shortCommitHash(): String {
  return git()?.commit()?.name?.substring(0, 8) ?: "unknown"
}

fun Project.git(): IndraGitExtension? = rootProject.extensions.findByType()

fun Project.sourceSets(): SourceSetContainer = the<JavaPluginExtension>().sourceSets

fun ProjectDependency.sourceSets(): SourceSetContainer = dependencyProject.sourceSets()

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

fun Project.exportLanguageFileInformation(): String {
  val file = project.buildDir.resolve("languages.txt")
  file.writeText(project.projectDir.resolve("src/main/resources/lang").listFiles()?.joinToString(separator = "\n") { it.name }!!)

  return file.absolutePath
}

fun Project.exportCnlFile(fileName: String, ignoredDependencyGroups: Array<String> = arrayOf()): String {
  val stringBuilder = StringBuilder("# CloudNet ${Versions.cloudNetCodeName} ${Versions.cloudNet}\n\n")
    .append("# repositories\n")
  // add all repositories
  mavenRepositories().forEach { repo ->
    stringBuilder.append("repo ${repo.name} ${repo.url.toString().dropLastWhile { it == '/' }}\n")
  }

  // add all dependencies
  stringBuilder.append("\n\n# dependencies\n")
  configurations.getByName("runtimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
    // get the module version from the artifact, stop if the dependency is ignored
    val id = it.moduleVersion.id
    if (id.group.equals(group) || ignoredDependencyGroups.contains(id.group)) {
      return@forEach
    }

    // check if the dependency is a snapshot version - in this case we need to use another artifact url
    var version = id.version
    if (id.version.endsWith("-SNAPSHOT") && it.id.componentIdentifier is MavenUniqueSnapshotComponentIdentifier) {
      // little hack to get the timestamped ("snapshot") version of the identifier
      version = (it.id.componentIdentifier as MavenUniqueSnapshotComponentIdentifier).timestampedVersion
    }

    // try to find the repository associated with the module
    val repository = resolveRepository(
      "${id.group.replace('.', '/')}/${id.name}/${id.version}/${id.name}-$version.jar",
      mavenRepositories()
    ) ?: throw IllegalStateException("Unable to resolve repository for $id")

    // add the repository
    val cs = ChecksumHelper.fileShaSum(it.file)
    stringBuilder.append("include ${repository.name} ${id.group} ${id.name} ${id.version} $version $cs ${it.classifier ?: ""}\n")
  }

  // write to the output file
  val target = project.buildDir.resolve(fileName)
  target.writeText(stringBuilder.toString())

  return target.absolutePath
}

private fun resolveRepository(
  testUrlPath: String,
  repositories: Iterable<MavenArtifactRepository>
): MavenArtifactRepository? {
  return repositories.firstOrNull {
    val url = URL(it.url.toURL(), testUrlPath)
    with(url.openConnection() as HttpURLConnection) {
      useCaches = false
      readTimeout = 30000
      connectTimeout = 30000
      instanceFollowRedirects = true

      setRequestProperty(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
      )

      connect()
      responseCode == 200
    }
  }
}
