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

import eu.cloudnetservice.cloudnet.gradle.util.CustomConfigurations
import eu.cloudnetservice.cloudnet.gradle.util.configurePublishing
import eu.cloudnetservice.cloudnet.gradle.util.sourceSets
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class CloudNetPublishPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      apply<CloudNetPlugin>()

      // these are the plugins which we need to apply to all projects
      apply(plugin = "signing")
      apply(plugin = "maven-publish")

      // all these projects are publishing their java artifacts
      // must happen after repository/dependency declaration
      afterEvaluate {
        plugins.withId("java") {
          configurePublishing("java")

          // create consumable artifacts for global javadoc
          configurations.consumable(CustomConfigurations.GLOBAL_JAVADOC_SOURCES) {
            outgoing.artifacts(sourceSets().named("main").map { it.allJava.srcDirs })
          }
          configurations.consumable(CustomConfigurations.GLOBAL_JAVADOC_CLASSPATH) {
            outgoing.artifacts(sourceSets().named("main").map { it.compileClasspath })
          }
        }
      }
    }
  }
}
