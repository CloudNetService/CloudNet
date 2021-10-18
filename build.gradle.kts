/*
 * Copyright 2019-2021 CloudNetService team & contributors
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
  if (name == "cloudnet-modules" || name == "cloudnet-plugins") return@subprojects

  apply(plugin = "java")
  apply(plugin = "checkstyle")
  apply(plugin = "java-library")
  apply(plugin = "maven-publish")
  apply(plugin = "org.cadixdev.licenser")

  dependencies {
    // lombok
    "compileOnly"("org.projectlombok", "lombok", Versions.lombok)
    "annotationProcessor"("org.projectlombok", "lombok", Versions.lombok)
    // annotations
    "compileOnly"("org.jetbrains", "annotations", Versions.annotations)
    // testing
    "testRuntimeOnly"("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)
    "testImplementation"("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
    "testImplementation"("org.junit.jupiter", "junit-jupiter-params", Versions.junit)
    "testImplementation"("org.mockito", "mockito-junit-jupiter", Versions.mockito)
  }

  tasks.withType<Jar> {
    from(rootProject.file("LICENSE"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
    // options
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.compilerArgs = listOf("-Xlint:deprecation", "-Xlint:unchecked")
  }
/*
  tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
  }
*/
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
    toolVersion = Versions.checkstyle
  }

  extensions.configure<LicenseExtension> {
    include("**/*.java")
    header(rootProject.file("LICENSE_HEADER"))
  }
}

/*

task allJavadoc(type: Javadoc) {
  dependsOn ":cloudnet-launcher:jar"

  destinationDir = new File(buildDir, "javadoc")
  title = "CloudNet documentation " + version

  options.encoding = "UTF-8"
  options.windowTitle = "CloudNet Javadocs"
  options.memberLevel = JavadocMemberLevel.PRIVATE
  options.addBooleanOption "-no-module-directories", true

  options.addStringOption("Xdoclint:none", "-quiet")

  def exportedProjects = subprojects.findAll {
    it.name != "cloudnet-modules" && it.name != "cloudnet-plugins"
  }.collect { it.path }

  source = exportedProjects.collect { project(it).sourceSets.main.allJava }
  classpath = files(exportedProjects.collect { project(it).sourceSets.main.compileClasspath })
  failOnError = false
}
*/
