/*
 * Copyright 2019-present CloudNetService team & contributors
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
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

  @get:Inject
  internal abstract val layout: ProjectLayout

  init {
    // Evaluate group lazily
    projectGroup.set(project.provider { project.group.toString() })
    outputFile.convention(layout.file(fileName.map { temporaryDir.resolve(it) }))
    mavenRepositories.convention(
      project.repositories.filterIsInstance<MavenArtifactRepository>().map { CacheableMavenRepository(it) }.toList()
    )
  }

  fun setResolvedArtifacts(runtimeClasspath: Provider<Configuration>) {
    val artifactsProvider = runtimeClasspath.map { cfg ->
      val view = cfg.incoming.artifactView {
        isLenient = true
        componentFilter { id -> id !is ProjectComponentIdentifier }
      }
      view.artifacts.artifacts
    }
    val mapped = artifactsProvider.map { set ->
      set.map { ar ->
        CacheableResolvedArtifact(id = ar.id, variant = ar.variant, file = ar.file)
      }
    }
    dependencies.set(mapped)
  }

  @TaskAction
  fun run() {
    val resolvedArtifacts = ArrayList<ResolvedArtifact>()
    val projectGroup = projectGroup.get()
    dependencies.get().forEach {
      val id = it.id
      val componentIdentifier = id.componentIdentifier
      if (componentIdentifier !is ModuleComponentIdentifier) {
        // catch unexpected component identifiers
        error("Unexpected dependency type: $componentIdentifier: ${componentIdentifier.javaClass.name}")
      }

      val group = componentIdentifier.moduleIdentifier.group
      if (group == projectGroup) return@forEach // no need to download CloudNet dependencies

      val version = componentIdentifier.version
      val name = componentIdentifier.moduleIdentifier.name

      val timestampedVersion = when (componentIdentifier is MavenUniqueSnapshotComponentIdentifier) {
        true -> componentIdentifier.timestampedVersion
        false -> version
      }
      val classifier = when (id is DefaultModuleComponentArtifactIdentifier) {
        true -> id.name.classifier
        false -> null
      }

      resolvedArtifacts.add(ResolvedArtifact(group, name, version, timestampedVersion, classifier, it.file))
    }

    val ignoredDependencyGroups = this.ignoredDependencyGroups.get()
    val mavenRepositories = ArrayList(this.mavenRepositories.get())
    val cnlFileContentBuilder = StringBuilder("### AUTOGENERATED FILE - DO NOT EDIT ###\n\n")

    // add all repositories
    mavenRepositories.forEach { repo ->
      cnlFileContentBuilder.append("repo ${repo.name} ${repo.url.dropLastWhile { it == '/' }}\n")
    }

    val deps = resolvedArtifacts.filter { !ignoredDependencyGroups.contains(it.group) }
    runBlocking {
      deps.map {
        async {
          // try to find the repository associated with the module
          val path = "${it.group.replace('.', '/')}/${it.name}/${it.version}/${it.name}-${it.timestampedVersion}.jar"
          val repository = resolveRepository(path, mavenRepositories) ?: throw IllegalStateException(
            "Unable to resolve repository for $it. Searched in ${mavenRepositories.joinToString(separator = "\n") { r -> r.url + path }}"
          )

          // add the repository
          val checksum = ChecksumHelper.fileShaSum(it.file)
          "include ${repository.name} ${it.group} ${it.name} ${it.version} ${it.timestampedVersion} $checksum ${it.classifier ?: ""}\n"
        }
      }.awaitAll().forEach {
        cnlFileContentBuilder.append(it)
      }
    }

    // write to the output file
    outputFile.get().asFile.writeText(cnlFileContentBuilder.toString())
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
  ): CacheableMavenRepository? = withContext(Dispatchers.IO) {
    supervisorScope {
      val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
      httpClient.use {
        val jobs = repositories.map {
          async {
            runCatching {
              val uri = URI.create("${it.url}/${testUrlPath}").normalize()
              val request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(
                  "User-Agent",
                  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
                )
                .build()
              val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
              when (response.statusCode() == 200) {
                true -> it
                else -> null
              }
            }.getOrNull()
          }
        }
        jobs.awaitAll().firstOrNull { it != null }
      }
    }
  }
}
