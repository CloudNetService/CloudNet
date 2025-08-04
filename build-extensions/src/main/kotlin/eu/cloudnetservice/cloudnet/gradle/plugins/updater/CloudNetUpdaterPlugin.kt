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

package eu.cloudnetservice.cloudnet.gradle.plugins.updater

import eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetJavaPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register

class CloudNetUpdaterPlugin : Plugin<Project> {
  companion object {
    const val UPDATER_DATA = "updater-data"
  }

  override fun apply(project: Project) {
    if (project.rootProject == project) {
      project.configureRootProject()
    } else {
      project.configureSubProject()
    }
  }

  fun Project.configureSubProject() {
    apply<CloudNetJavaPlugin>()

    val updaterData = configurations.register("updaterData") {
      attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(UPDATER_DATA))
      }
    }

    val prepareUpdaterData = tasks.register<PrepareUpdaterDataTask>("prepareUpdaterData") {
      into(temporaryDir)
    }

    artifacts.add(updaterData.name, prepareUpdaterData)
  }

  fun Project.configureRootProject() {
    val updaterDependencies = configurations.register("updaterDependencies") {
      attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(UPDATER_DATA))
      }
    }

    subprojects.map { it.isolated }.forEach {
      dependencies.add(
        updaterDependencies.name, dependencies.project(path = it.path)
      )
    }

    val view = updaterDependencies.map { it.lenientView }
    val artifacts = view.map { it.files }
    val genUpdaterInformation = tasks.register<GenerateUpdaterInformationTask>("genUpdaterInformation")
    genUpdaterInformation.configure {
      this.artifacts.from(artifacts)
      this.destinationDirectory.set(project.layout.projectDirectory.dir(".launchermeta"))
    }
  }
}

val Configuration.lenientView
  get() = this.incoming.artifactView { isLenient = true }
