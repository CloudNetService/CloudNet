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

package eu.cloudnetservice.cloudnet.gradle.plugins

import eu.cloudnetservice.cloudnet.gradle.util.library
import eu.cloudnetservice.cloudnet.gradle.util.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.repositories

class CloudNetPluginsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      apply<CloudNetJavaPlugin>()

      repositories {
        maven("https://repo.waterdog.dev/releases/")
        maven("https://repo.waterdog.dev/snapshots/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://repo.loohpjames.com/repository")
        maven("https://repo.opencollab.dev/maven-releases/")
        maven("https://repo.opencollab.dev/maven-snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
      }

      dependencies {
        "implementation"(libs.library("guava"))

        // generation for platform main classes
        "compileOnly"(project(":ext:platform-inject-support:platform-inject-api"))
        "annotationProcessor"(project(":ext:platform-inject-support:platform-inject-processor"))
      }

      registerProcessSources()
    }
  }
}
