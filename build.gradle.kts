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
  alias(libs.plugins.versions)
  alias(libs.plugins.licenser)
  alias(libs.plugins.nexusPublish)
}

defaultTasks("build", "checkLicenses", "test", "shadowJar")

allprojects {
  version = Versions.cloudNet
  group = "eu.cloudnetservice.cloudnet"
  description = "The alternative Minecraft Java and Bedrock server management solution"

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
  ) {
    return@subprojects
  }

  // these are the plugins which we need to apply to all projects
  apply(plugin = "signing")
  apply(plugin = "maven-publish")

  // skip further applying to bom - this project is a bit special as we're not allowed to
  // apply the java plugin to it (that's why we need to stop here, but we need to publish
  // at well (that's why we're applying the publish plugin)
  if (name == "cloudnet-bom") {
    return@subprojects
  }

  apply(plugin = "checkstyle")
  apply(plugin = "java-library")
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

  extensions.configure<CheckstyleExtension> {
    toolVersion = Versions.checkstyleTools
  }

  extensions.configure<LicenseExtension> {
    include("**/*.java")
    header(rootProject.file("LICENSE_HEADER"))
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

    // options
    options.encoding = "UTF-8"
    options.memberLevel = JavadocMemberLevel.PRIVATE
    options.addStringOption("-html5")
    options.addBooleanOption("Xdoclint:none", true) // TODO: enable when we're done with javadocs
  }

  // all these projects are publishing their java artifacts
  configurePublishing("java", true)
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

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

      username.set(System.getenv("sonatypeUsername"))
      password.set(System.getenv("sonatypePassword"))
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
