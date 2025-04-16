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

plugins {
  id("cloudnet.parent-build-logic")
  alias(libs.plugins.nexusPublish)
  alias(libs.plugins.shadow) apply false // must be here to enforce the bundled asm version
  alias(libs.plugins.spotless) apply false
}

defaultTasks("build")

tasks.register("globalJavaDoc", Javadoc::class) {
  val options = options as? StandardJavadocDocletOptions ?: return@register

  title = "CloudNet JavaDocs"
  setDestinationDir(layout.buildDirectory.dir("javadocs").get().asFile)
  // options
  applyDefaultJavadocOptions(options)
  options.windowTitle = "CloudNet JavaDocs"
  // set the sources
  val sources = subprojects.filter { it.plugins.hasPlugin("java") }.map { it.path }
  source(files(sources.flatMap { project(it).sourceSets()["main"].allJava }))
  classpath = files(sources.flatMap { project(it).sourceSets()["main"].compileClasspath })
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

libs.lombok

gradle.projectsEvaluated {
  tasks.register("genUpdaterInformation") {
    subprojects.forEach {
      // check if we need to depend on the plugin
      if (!it.plugins.hasPlugin("java")) return@forEach
      // depend this task on the build output of each subproject
      dependsOn("${it.path}:build")
    }
    // generate the updater information
    doLast {
      generateUpdaterInformation()
    }
  }
}
