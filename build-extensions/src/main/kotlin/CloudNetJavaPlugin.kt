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

import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.filter
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType

class CloudNetJavaPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      apply<CloudNetPlugin>()
      apply<CloudNetPublishPlugin>()

      apply(plugin = "checkstyle")
      apply(plugin = "java-library")
      apply(plugin = "com.diffplug.spotless")
//      apply(plugin = "net.kyori.indra.git")

      // Add common repositories required in all projects
      this.addDefaultRepositories()
      // Add common dependencies required in all projects
      this.addDefaultDependencies()

      this.includeLicenseInJar()

      // By default, all projects use the latest java version.
      // This may be overwritten by API-projects
      this.configureJavaVersion(Versions.javaVersion)

      this.configureTestTasks()

      this.configureCompileJava()

      afterEvaluate {
        // Configure shadow after the subproject has been able to even apply it
        this.configureShadow()
      }

      this.configureCheckstyle()

      this.configureSpotless()

      // TODO find a better name
      this.configureJavaVersionTasks()
    }
  }
}

// We need the jvmArgs to run JVMs in multiple places
private val cloudNetJvmArgs = arrayOf(
  "--enable-preview",
  "-XX:+EnableDynamicAgentLoading",
  "--enable-native-access=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
)

private fun Project.addDefaultRepositories() {
  repositories {
    releasesOnly(mavenCentral())
    snapshotsOnly(maven("https://central.sonatype.com/repository/maven-snapshots/"))

    // ensure that we use these repositories for snapshots/releases only (improves lookup times)
    releasesOnly(maven("https://repository.derklaro.dev/releases/"))
    snapshotsOnly(maven("https://repository.derklaro.dev/snapshots/"))

    // must be after sonatype as sponge mirrors sonatype which leads to outdated dependencies
    maven("https://repo.spongepowered.org/maven/")
  }
}

/**
 * Adds default dependencies like annotation processors and testing
 */
private fun Project.addDefaultDependencies() {
  val libs = this.libs

  afterEvaluate {
    dependencies {
      // lombok
      "compileOnly"(libs.library("lombok"))
      "annotationProcessor"(libs.library("lombok"))
      // annotations
      "compileOnly"(libs.library("annotations"))
      // testing
      "testImplementation"(libs.library("mockito"))
      "testRuntimeOnly"(libs.library("junitLauncher"))
      "testImplementation"(libs.bundle("junit"))
      "testImplementation"(libs.bundle("testContainers"))
    }
  }

  configurations.configureEach {
    // unsure why but every project loves them, and they literally have an import for every letter I type - beware
    exclude("org.checkerframework", "checker-qual")
  }
}

private fun Project.includeLicenseInJar() {
  tasks.withType<Jar>().configureEach {
    from(isolated.rootProject.projectDirectory.file("LICENSE"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
}

private fun Project.configureTestTasks() {
  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
      events("started", "passed", "skipped", "failed")
    }

    // allow dynamic agent loading for mockito
    jvmArgs(*cloudNetJvmArgs)

    // always pass down all given system properties
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
  }
}

private fun Project.configureCompileJava() {
  tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isIncremental = true

    if (project.path != ":launcher:java8" && project.path != ":launcher:patcher" && !project.path.contains("api")) {
      options.compilerArgs.add("--enable-preview")
      options.compilerArgs.add("-Xlint:-deprecation,-unchecked,-preview")
      options.compilerArgs.add("-proc:full")
    }
  }
}

private fun Project.configureShadow() {
  if (plugins.hasPlugin(ShadowJavaPlugin::class)) {
    tasks.named("assemble").configure {
      dependsOn(tasks.named("shadowJar"))
    }
    tasks.named<Jar>("jar").configure {
      // we use the shadow jar task, so move the jar into the task's temporary dir to avoid clutter
      destinationDirectory = temporaryDir
    }
  }
}

/**
 * This may seem somewhat odd.
 * When running JavaExec tasks from IntelliJ, sometimes to quickly run a snippet of code with a main() method,
 * the JavaExec tasks will often have the wrong java version and fail. So we override them
 */
private fun Project.configureJavaVersionTasks() {
  // This must be inside withPlugin("java-base") or else the extensions won't be registered yet
  pluginManager.withPlugin("java-base") {
    val extension = extensions.getByType<JavaPluginExtension>()
    val service = extensions.getByType<JavaToolchainService>()
    val launcher = service.launcherFor(extension.toolchain)
    val latestLauncher = service.launcherFor { this.configureFor(Versions.javaVersion) }
    val javadoc = service.javadocToolFor { this.configureFor(Versions.javaVersion) }

    tasks.withType<Checkstyle>().configureEach {
      javaLauncher = latestLauncher
    }

    tasks.withType<Test>().configureEach {
      javaLauncher = latestLauncher
    }

    tasks.withType<JavaExec>().configureEach {
      javaLauncher = launcher
      jvmArgs(*cloudNetJvmArgs)
    }

    tasks.withType<Javadoc>().configureEach {
      javadocTool = javadoc
      options.source = Versions.javaVersion.asInt().toString()
    }
  }
}

private fun Project.configureCheckstyle() {

  tasks.withType<Checkstyle>().configureEach {
    maxErrors = 0
    maxWarnings = 0
    configFile = project.isolated.rootProject.projectDirectory.file("checkstyle.xml").asFile
  }

  // TODO investigate why this afterEvaluate exists
  afterEvaluate {
    extensions.configure<CheckstyleExtension> {
      toolVersion = libs.map { it.findVersion("checkstyleTools").orElseThrow().requiredVersion }.get()
    }
  }
}

private fun Project.configureSpotless() {
  extensions.configure<SpotlessExtension> {
    java {
      licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
    }
  }
}

// ----------------------------------------------------
// -          helpers used in other plugins           -
// ----------------------------------------------------

internal fun Project.registerProcessSources() {
  val processSources = tasks.register<Sync>("processSources") {
    inputs.property("version", project.version)
    from(sourceSets().named("main").map { it.java })
    into(layout.buildDirectory.dir("src"))
    filter(ReplaceTokens::class, mapOf("tokens" to mapOf("version" to project.version)))
  }
  tasks.named<JavaCompile>("compileJava") {
    dependsOn(processSources)
    setSource(processSources)
  }
}

// ----------------------------------------------------
// -               publicly visible api               -
// ----------------------------------------------------

fun JavaToolchainSpec.configureFor(version: JavaLanguageVersion) {
  this.languageVersion = version
  this.vendor = JvmVendorSpec.ADOPTIUM
}

fun Project.configureJavaVersion(version: JavaLanguageVersion) {
  this.extensions.configure<JavaPluginExtension> {
    this.toolchain.configureFor(version)
  }
}
