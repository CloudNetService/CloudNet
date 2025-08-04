/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

import eu.cloudnetservice.cloudnet.gradle.applyDefaultJavadocOptions
import eu.cloudnetservice.cloudnet.gradle.plugins.configureFor
import eu.cloudnetservice.cloudnet.gradle.plugins.updater.lenientView
import eu.cloudnetservice.cloudnet.gradle.util.CustomConfigurations
import eu.cloudnetservice.cloudnet.gradle.util.Versions

plugins {
  id("cloudnet")
  id("cloudnet-updater")
  id("java-base")
  alias(libs.plugins.nexusPublish)
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.shadow) apply false
  alias(libs.plugins.fabricLoom) apply false
}

defaultTasks("build")

val globalJavadocSources = configurations.register("globalJavadocSources")
val globalJavadocClasspath = configurations.register("globalJavadocClasspath")

tasks.register("globalJavaDoc", Javadoc::class) {
  val options = options as? StandardJavadocDocletOptions ?: return@register
  javadocTool = javaToolchains.javadocToolFor { configureFor(Versions.javaVersion) }
  options.source = Versions.javaVersion.asInt().toString()

  title = "CloudNet JavaDocs"
  destinationDir = layout.buildDirectory.dir("javadocs").get().asFile
  // options
  applyDefaultJavadocOptions(options)
  options.windowTitle = "CloudNet JavaDocs"
  // set the sources. We are using lenientView to ignore subprojects that shouldn't be included
  source(globalJavadocSources.map { it.lenientView.files })
  classpath = globalJavadocClasspath.map { it.lenientView.files }.get()
}

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
      snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))

      username.set(System.getenv("CENTRAL_USER"))
      password.set(System.getenv("CENTRAL_PASSWORD"))
    }
  }

  useStaging.set(!project.version.toString().endsWith("-SNAPSHOT"))
}

dependencies {
  subprojects.map { it.isolated }.forEach { project ->
    globalJavadocSources(this.project(project.path, CustomConfigurations.GLOBAL_JAVADOC_SOURCES))
    globalJavadocClasspath(this.project(project.path, CustomConfigurations.GLOBAL_JAVADOC_CLASSPATH))
  }
}

// Adapted from https://stackoverflow.com/a/75923728 to fix running inside IDE
gradle.taskGraph.whenReady {
  allTasks.filterIsInstance<JavaExec>().forEach {
    it.setExecutable(it.javaLauncher.get().executablePath.asFile.absolutePath)
  }
}
