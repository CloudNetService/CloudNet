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

import eu.cloudnetservice.gradle.juppiter.JuppiterPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.repositories

class CloudNetModulesPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      apply<CloudNetJavaPlugin>()
      apply<JuppiterPlugin>()

      configurations {
        named("testImplementation").configure {
          extendsFrom(getByName("moduleLibrary"))
        }
      }

      configureModules()
    }
  }
}
