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

import eu.cloudnetservice.cloudnet.gradle.util.ChecksumHelper
import eu.cloudnetservice.cloudnet.gradle.util.Versions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
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
  val dependencies: ListProperty<CacheableResolvedArtifact> = objects.listProperty()

  @Nested
  val mavenRepositories: ListProperty<CacheableMavenRepository> = objects.listProperty()

  @Input
  val projectGroup: Property<String> = objects.property()

  @get: Inject
  internal abstract val layout: ProjectLayout

  private val repositoryComparator =
    Comparator.comparing<CacheableMavenRepository, String> { it.name }.thenComparing { it.url }

  init {
    // Evaluate group lazily
    projectGroup.set(project.provider { project.group.toString() })
    outputFile.convention(layout.file(fileName.map { temporaryDir.resolve(it) }))
    mavenRepositories.convention(
      project.repositories.filterIsInstance<MavenArtifactRepository>().map { CacheableMavenRepository(it) }.toList()
    )
  }

  fun setResolvedArtifacts(runtimeClasspath: Provider<Configuration>) {
    val resolved = runtimeClasspath.flatMap { it.incoming.artifacts.resolvedArtifacts }

    dependencies.set(resolved.map { set ->
      set.map {
        val file = it.file
        val id = it.id
        val variant = it.variant
        CacheableResolvedArtifact(id, variant, file)
      }.filter {
        // Filter out all subprojects
        it.id.componentIdentifier !is ProjectComponentIdentifier
      }
    })
  }

  @TaskAction
  fun run() {
    val resolvedArtifacts = ArrayList<ResolvedArtifact>()
    val projectGroup = projectGroup.get()
    dependencies.get().forEach { it ->
      val id = it.id
      val componentIdentifier = id.componentIdentifier

      if (componentIdentifier !is ModuleComponentIdentifier) {
        // Throw an error to make this future-proof. In case any weird unsupported new dependency is introduced,
        // this will catch that.
        error("Unexpected dependency type: $componentIdentifier: ${componentIdentifier.javaClass.name}")
      }

      val group = componentIdentifier.moduleIdentifier.group
      // Ignore all CloudNet projects
      if (group == projectGroup) return@forEach
      val name = componentIdentifier.moduleIdentifier.name
      val version = componentIdentifier.version
      val file = it.file

      val timestampedVersion =
        if (version.endsWith("-SNAPSHOT") && componentIdentifier is MavenUniqueSnapshotComponentIdentifier) {
          // little hack to get the timestamped ("snapshot") version of the identifier
          componentIdentifier.timestampedVersion
        } else version

      val classifier = if (id is DefaultModuleComponentArtifactIdentifier) {
        // hack to get the classifier
        id.name.classifier
      } else ""

      resolvedArtifacts.add(ResolvedArtifact(group, name, version, timestampedVersion, classifier, file))
    }

    val ignoredDependencyGroups = this.ignoredDependencyGroups.get()
    val mavenRepositories = ArrayList(this.mavenRepositories.get()).apply { sortWith(repositoryComparator) }

    val stringBuilder =
      StringBuilder("# CloudNet ${Versions.cloudNetCodeName} ${Versions.cloudNet}\n\n").append("# repositories\n")
    // add all repositories
    mavenRepositories.forEach { repo ->
      stringBuilder.append("repo ${repo.name} ${repo.url.dropLastWhile { it == '/' }}\n")
    }

    // add all dependencies
    stringBuilder.append("\n\n# dependencies\n")

    val deps = resolvedArtifacts.filter { !ignoredDependencyGroups.contains(it.group) }

    runBlocking {
      deps.map {
        async {
          // try to find the repository associated with the module
          val path = "${it.group.replace('.', '/')}/${it.name}/${it.version}/${it.name}-${it.timestampedVersion}.jar"
          val repository =
            resolveRepository(path, mavenRepositories) ?: throw IllegalStateException(
              "Unable to resolve repository for $it.\nSearched in ${
                mavenRepositories.joinToString(
                  separator = "\n"
                ) { r -> r.url + path }
              }")

          // add the repository
          val cs = ChecksumHelper.fileShaSum(it.file)
          "include ${repository.name} ${it.group} ${it.name} ${it.version} ${it.timestampedVersion} $cs ${it.classifier ?: ""}\n"
        }
      }.awaitAll().forEach {
        stringBuilder.append(it)
      }
    }

    // write to the output file
    outputFile.get().asFile.writeText(stringBuilder.toString())
  }

  data class CacheableResolvedArtifact(
    @Input val id: ComponentArtifactIdentifier,
    @Input val variant: ResolvedVariantResult,
    @InputFile @PathSensitive(PathSensitivity.RELATIVE) val file: File
  )

  private data class ResolvedArtifact(
    val group: String,
    val name: String,
    val version: String,
    val timestampedVersion: String,
    val classifier: String?,
    val file: File
  )

  data class CacheableMavenRepository(@Input val name: String, @Input val url: String) {
    constructor(repository: MavenArtifactRepository) : this(repository.name, repository.url.toString())
  }

  private suspend fun resolveRepository(
    testUrlPath: String, repositories: Iterable<CacheableMavenRepository>
  ): CacheableMavenRepository? {
    return withContext(Dispatchers.IO) {
      repositories.firstOrNull {
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
  }
}
