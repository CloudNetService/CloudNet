/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  id("cloudnet.parent-build-logic")
  alias(libs.plugins.spotless)
  alias(libs.plugins.nexusPublish)
  alias(libs.plugins.fabricLoom) apply false
}

defaultTasks("build", "test", "shadowJar")

allprojects {
  version = Versions.cloudNet
  group = "eu.cloudnetservice.cloudnet"
  description = "A modern application that can dynamically and easily deliver Minecraft oriented software"

  repositories {
    releasesOnly(mavenCentral())

    // old and new sonatype snapshot repository
    snapshotsOnly(maven("https://oss.sonatype.org/content/repositories/snapshots/"))
    snapshotsOnly(maven("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

    // must be after sonatype as sponge mirrors sonatype which leads to outdated dependencies
    maven("https://repo.spongepowered.org/maven/")

    // ensure that we use these repositories for snapshots/releases only (improves lookup times)
    releasesOnly(maven("https://repository.derklaro.dev/releases/"))
    snapshotsOnly(maven("https://repository.derklaro.dev/snapshots/"))
  }
}

subprojects {
  // these are top level projects which are configured separately
  if (name == "modules" || name == "plugins" || name == "ext" || name == "launcher") {
    return@subprojects
  }

  // these are the plugins which we need to apply to all projects
  apply(plugin = "signing")
  apply(plugin = "maven-publish")

  // skip further applying to bom - this project is a bit special as we're not allowed to
  // apply the java plugin to it (that's why we need to stop here, but we need to publish
  // at well (that's why we're applying the publish plugin)
  if (name == "bom") {
    return@subprojects
  }

  apply(plugin = "checkstyle")
  apply(plugin = "java-library")
  apply(plugin = "com.diffplug.spotless")

  dependencies {
    // the 'rootProject.libs.' prefix is needed here - see https://github.com/gradle/gradle/issues/16634
    // lombok
    "compileOnly"(rootProject.libs.lombok)
    "annotationProcessor"(rootProject.libs.lombok)
    // annotations
    "compileOnly"(rootProject.libs.annotations)
    // testing
    "testImplementation"(rootProject.libs.mockito)
    "testImplementation"(rootProject.libs.bundles.junit)
    "testImplementation"(rootProject.libs.bundles.testContainers)
  }

  configurations.all {
    // unsure why but every project loves them, and they literally have an import for every letter I type - beware
    exclude("org.checkerframework", "checker-qual")
  }

  tasks.withType<Jar> {
    from(rootProject.file("LICENSE"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
      events("started", "passed", "skipped", "failed")
    }
    // always pass down all given system properties
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
  }

  tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
    // options
    options.encoding = "UTF-8"
    options.isIncremental = true
    // we are aware that those are there, but we only do that if there is no other way we can use - so please keep the terminal clean!
    options.compilerArgs = mutableListOf("-Xlint:-deprecation,-unchecked")
  }

  tasks.withType<Checkstyle> {
    maxErrors = 0
    maxWarnings = 0
    configFile = rootProject.file("checkstyle.xml")
  }

  extensions.configure<CheckstyleExtension> {
    toolVersion = rootProject.libs.versions.checkstyleTools.get()
  }

  extensions.configure<SpotlessExtension> {
    java {
      licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
    }
  }

  tasks.register<org.gradle.jvm.tasks.Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.getByName("javadoc"))
  }

  tasks.register<org.gradle.jvm.tasks.Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(project.sourceSets()["main"].allJava)
  }

  tasks.withType<Javadoc> {
    val options = options as? StandardJavadocDocletOptions ?: return@withType
    applyDefaultJavadocOptions(options)
  }

  // all these projects are publishing their java artifacts
  configurePublishing("java", true)
}

tasks.register("globalJavaDoc", Javadoc::class) {
  val options = options as? StandardJavadocDocletOptions ?: return@register

  title = "CloudNet JavaDocs"
  setDestinationDir(buildDir.resolve("javadocs"))
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
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

      username.set(System.getenv("SONATYPE_USER"))
      password.set(System.getenv("SONATYPE_TOKEN"))
    }
  }

  useStaging.set(!project.version.toString().endsWith("-SNAPSHOT"))
}

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
