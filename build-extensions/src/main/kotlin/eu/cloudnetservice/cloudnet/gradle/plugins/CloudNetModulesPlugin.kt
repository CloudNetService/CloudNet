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

import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import eu.cloudnetservice.cloudnet.gradle.plugins.updater.CloudNetUpdaterPlugin
import eu.cloudnetservice.cloudnet.gradle.tasks.PrepareUpdaterDataTask
import eu.cloudnetservice.cloudnet.gradle.util.UpdaterMeta
import eu.cloudnetservice.gradle.juppiter.GenerateModuleJson
import eu.cloudnetservice.gradle.juppiter.JuppiterPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property

class CloudNetModulesPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      apply<CloudNetJavaPlugin>()
      apply<CloudNetUpdaterPlugin>()
      apply<JuppiterPlugin>()

      configurations {
        named("testImplementation").configure {
          extendsFrom(getByName("moduleLibrary"))
        }
      }

      val generateModuleJson = tasks.named<GenerateModuleJson>("genModuleJson")

      val archiveFileName = project.objects.property<String>()
      val prepareUpdaterData = tasks.named<PrepareUpdaterDataTask>("prepareUpdaterData") {
        dependsOn(generateModuleJson)

        from(generateModuleJson.flatMap { it.outputDirectory.file(it.fileName) })

        val moduleJsonName = generateModuleJson.flatMap { it.fileName }

        meta.set(archiveFileName.flatMap { archiveName ->
          moduleJsonName.map { moduleJsonName ->
            UpdaterMeta(UpdaterMeta.Type.MODULE, UpdaterMeta.Data.Module(archiveName, moduleJsonName))
          }
        })
      }

      afterEvaluate {
        val archiveProducer = (if (plugins.hasPlugin(ShadowJavaPlugin::class.java)) {
          tasks.named<Jar>("shadowJar")
        } else if (plugins.hasPlugin("fabric-loom")) {
          tasks.named<Jar>("remapJar")
        } else {
          tasks.named<Jar>("jar")
        })

        prepareUpdaterData.configure {
          archiveFileName.convention(fromArchive(archiveProducer))
        }
      }

      configureModules()
    }
  }
}
