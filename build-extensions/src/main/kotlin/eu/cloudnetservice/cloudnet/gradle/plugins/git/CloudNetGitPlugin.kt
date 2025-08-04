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

package eu.cloudnetservice.cloudnet.gradle.plugins.git

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import javax.inject.Inject

class CloudNetGitPlugin : Plugin<Project> {
  companion object {
    private const val EXTENSION_NAME = "git"
    private const val SERVICE_NAME = "gitService"
  }

  override fun apply(project: Project) {
    val rootDir = project.rootDir
    val service = project.gradle.sharedServices.registerIfAbsent(SERVICE_NAME, GitService::class.java) {
      parameters.projectDirectory.set(rootDir)
    }

    project.extensions.create(GitExtension::class.java, EXTENSION_NAME, GitExtensionImpl::class.java, service, project)
  }
}

interface GitExtension {
  val project: Project
  val service: Provider<GitService>
}

internal abstract class GitExtensionImpl @Inject constructor(final override val service: Provider<GitService>, final override val project: Project) :
  GitExtension
