/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import org.cadixdev.gradle.licenser.LicenseExtension

plugins {
  id("cloudnet.parent-build-logic")
  id("com.github.ben-manes.versions") version "0.40.0"
  id("org.cadixdev.licenser") version "0.6.1" apply false
}

defaultTasks("build", "checkLicenses", "test", "shadowJar")

allprojects {
  version = Versions.cloudNet
  group = "eu.cloudnetservice.cloudnet"

  repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.incendo.org/content/repositories/snapshots/")
  }
}

subprojects {
  // these are top level projects which are configured separately
  if (name == "cloudnet-modules"
    || name == "cloudnet-plugins"
    || name == "cloudnet-ext"
    || name == "cloudnet-launcher"
    || name == "cloudnet-bom"
  ) {
    return@subprojects
  }

  apply(plugin = "java")
  apply(plugin = "checkstyle")
  apply(plugin = "java-library")
  apply(plugin = "maven-publish")
  apply(plugin = "org.cadixdev.licenser")

  dependencies {
    // the 'rootProject.libs.' prefix is needed here - see https://github.com/gradle/gradle/issues/16634
    // lombok
    "compileOnly"(rootProject.libs.lombok)
    "annotationProcessor"(rootProject.libs.lombok)
    // annotations
    "compileOnly"(rootProject.libs.annotations)
    // testing
    "testImplementation"(rootProject.libs.bundles.junit)
    "testImplementation"(rootProject.libs.bundles.mockito)
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
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
    // options
    options.encoding = "UTF-8"
    options.isIncremental = true
  }

  tasks.withType<Checkstyle> {
    maxErrors = 0
    maxWarnings = 0
    configFile = rootProject.file("checkstyle.xml")
  }

  extensions.configure<JavaPluginExtension> {
    // withSourcesJar()
    // withJavadocJar()
  }

  extensions.configure<CheckstyleExtension> {
    toolVersion = Versions.checkstyleTools
  }

  extensions.configure<LicenseExtension> {
    include("**/*.java")
    header(rootProject.file("LICENSE_HEADER"))
    // third party library classes - keep their copyright
    exclude("**/eu/cloudnetservice/cloudnet/driver/util/define/**")
  }
}

tasks.register("globalJavaDoc", Javadoc::class) {
  val options = options as? StandardJavadocDocletOptions ?: return@register

  title = "CloudNet JavaDocs"
  setDestinationDir(buildDir.resolve("javadocs"))
  // options
  options.encoding = "UTF-8"
  options.windowTitle = "CloudNet JavaDocs"
  options.memberLevel = JavadocMemberLevel.PRIVATE
  options.addStringOption("-html5")
  options.addBooleanOption("Xdoclint:none", true) // TODO: enable when we're done with javadocs
  // set the sources
  val sources = subprojects.filter { it.plugins.hasPlugin("java") }.map { it.path }
  source(files(sources.flatMap { project(it).sourceSets()["main"].allJava }))
  classpath = files(sources.flatMap { project(it).sourceSets()["main"].compileClasspath })
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
