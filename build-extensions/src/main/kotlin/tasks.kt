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

@file:Suppress("LeakingThis")

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.setProperty
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import javax.inject.Inject

@CacheableTask
abstract class ExportCnlFile : DefaultTask() {
  @get:Inject
  abstract val objects: ObjectFactory

  @get:Input
  abstract val fileName: Property<String>

  @Input
  val ignoredDependencyGroups: SetProperty<String> = objects.setProperty()

  @OutputFile
  val outputFile: RegularFileProperty = objects.fileProperty()

  @Nested
  val mavenRepositories: ListProperty<CacheableMavenRepository> = objects.listProperty()

  @Nested
  val resolvedArtifacts: SetProperty<CacheableResolvedArtifact> = objects.setProperty()

  @get: Inject
  internal abstract val layout: ProjectLayout

  init {
    outputFile.convention(layout.file(fileName.map { temporaryDir.resolve(it) }))
    mavenRepositories.convention(project.mavenRepositories().map { CacheableMavenRepository(it) }.toList())
  }

  fun setResolvedArtifacts(runtimeClasspath: Configuration) {
    val artifacts = runtimeClasspath.resolvedConfiguration.resolvedArtifacts.map {
      val group = it.moduleVersion.id.group
      val name = it.moduleVersion.id.name
      val version = it.moduleVersion.id.version
      val timestampedVersion =
        if (version.endsWith("-SNAPSHOT") && it.id.componentIdentifier is MavenUniqueSnapshotComponentIdentifier) {
          // little hack to get the timestamped ("snapshot") version of the identifier
          (it.id.componentIdentifier as MavenUniqueSnapshotComponentIdentifier).timestampedVersion
        } else version

      val classifier = it.classifier
      val file = it.file
      CacheableResolvedArtifact(group, name, version, timestampedVersion, classifier, file)
    }.filter {
      it.group != project.group
    }.toSet()
    this.resolvedArtifacts.set(artifacts)
  }

  @TaskAction
  fun run() {
    val ignoredDependencyGroups = this.ignoredDependencyGroups.get()
    val mavenRepositories = this.mavenRepositories.get()

    val stringBuilder =
      StringBuilder("# CloudNet ${Versions.cloudNetCodeName} ${Versions.cloudNet}\n\n").append("# repositories\n")
    // add all repositories
    mavenRepositories.forEach { repo ->
      stringBuilder.append("repo ${repo.name} ${repo.url.dropLastWhile { it == '/' }}\n")
    }

    // add all dependencies
    stringBuilder.append("\n\n# dependencies\n")
    resolvedArtifacts.get().forEach {
      // get the module version from the artifact, stop if the dependency is ignored
      if (ignoredDependencyGroups.contains(it.group)) {
        return@forEach
      }

      // try to find the repository associated with the module
      val path = "${it.group.replace('.', '/')}/${it.name}/${it.version}/${it.name}-${it.timestampedVersion}.jar"
      val repository = resolveRepository(
        path, mavenRepositories
      ) ?: throw IllegalStateException(
        "Unable to resolve repository for $it.\nSearched in ${
          mavenRepositories.joinToString(
            separator = "\n"
          ) { r -> r.url + path }
        }")

      // add the repository
      val cs = ChecksumHelper.fileShaSum(it.file)
      stringBuilder.append("include ${repository.name} ${it.group} ${it.name} ${it.version} ${it.timestampedVersion} $cs ${it.classifier ?: ""}\n")
    }

    // write to the output file
    outputFile.get().asFile.writeText(stringBuilder.toString())
  }
}

data class CacheableResolvedArtifact(
  @Input val group: String,
  @Input val name: String,
  @Input val version: String,
  @Input val timestampedVersion: String,
  @Optional @Input val classifier: String?,
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE) val file: File
)

data class CacheableMavenRepository(@Input val name: String, @Input val url: String) {
  constructor(repository: MavenArtifactRepository) : this(repository.name, repository.url.toString())
}

private fun resolveRepository(
  testUrlPath: String, repositories: Iterable<CacheableMavenRepository>
): CacheableMavenRepository? {
  return repositories.firstOrNull {
    val url = URI.create(it.url).resolve(testUrlPath).toURL()
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

@CacheableTask
abstract class ExportLanguageFileInformation : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val languageFiles: ConfigurableFileCollection

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  init {
    outputFile.convention { temporaryDir.resolve("languages.txt") }
  }

  @TaskAction
  fun run() {
    outputFile.asFile.get().writeText(languageFiles.files.joinToString(separator = "\n") { it.name })
  }
}
