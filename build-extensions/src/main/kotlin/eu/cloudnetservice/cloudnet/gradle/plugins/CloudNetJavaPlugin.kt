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

import com.diffplug.gradle.spotless.SpotlessExtension
import eu.cloudnetservice.cloudnet.gradle.util.bundle
import eu.cloudnetservice.cloudnet.gradle.util.library
import eu.cloudnetservice.cloudnet.gradle.util.libs
import eu.cloudnetservice.cloudnet.gradle.util.releasesOnly
import eu.cloudnetservice.cloudnet.gradle.util.snapshotsOnly
import eu.cloudnetservice.cloudnet.gradle.util.sourceSets
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
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType

/**
 * Java version compatibility to apply to all non-api projects.
 */
val JAVA_CORE_COMPATIBILITY = JavaLanguageVersion.of(24)

class CloudNetJavaPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      apply<CloudNetPlugin>()

      apply(plugin = "checkstyle")
      apply(plugin = "java-library")
      apply(plugin = "net.kyori.indra.git")
      apply(plugin = "com.diffplug.spotless")

      this.addDefaultRepositories()
      this.addDefaultDependencies()

      this.includeLicenseInJar()

      // By default, all projects use the latest java version.
      // This may be overwritten by API-projects
      this.configureJavaVersion()

      this.configureTestTasks()
      this.configureCompileJava()
      this.configureShadow()
      this.configureCheckstyle()
      this.configureSpotless()
      this.applyJavaLauncher()
      this.extendTestImplementationFromCompileOnly()
    }
  }
}

// We need the jvmArgs to run JVMs in multiple places
private val cloudNetJvmArgs = arrayOf(
  "--enable-preview",
  "-XX:+EnableDynamicAgentLoading",
  "--enable-native-access=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "-Dio.netty5.noUnsafe=true",
)

private fun Project.addDefaultRepositories() {
  repositories {
    mavenCentral().releasesOnly()
    maven("https://central.sonatype.com/repository/maven-snapshots/").snapshotsOnly()

    // ensure that we use these repositories for snapshots/releases only (improves lookup times)
    maven("https://repository.derklaro.dev/releases/").releasesOnly()
    maven("https://repository.derklaro.dev/snapshots/").snapshotsOnly()

    // must be after sonatype as sponge mirrors sonatype which leads to outdated dependencies
    maven("https://repo.spongepowered.org/maven/")
  }
}

private fun Project.addDefaultDependencies() {
  val libs = this.libs

  afterEvaluate {
    dependencies {
      "compileOnly"(libs.library("lombok"))
      "annotationProcessor"(libs.library("lombok"))
      "compileOnly"(libs.library("annotations"))

      "testImplementation"(libs.library("mockito"))
      "testRuntimeOnly"(libs.library("junitLauncher"))
      "testImplementation"(libs.bundle("junit"))
      "testImplementation"(libs.bundle("testContainers"))
    }
  }

  configurations.configureEach {
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

    jvmArgs(*cloudNetJvmArgs)
    systemProperties(System.getProperties().mapKeys { it.key.toString() })
  }
}

private fun Project.configureCompileJava() {
  tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isIncremental = true

    if (project.path != ":launcher:launcher-patcher" && !project.plugins.hasPlugin("cloudnet-java-api")) {
      options.compilerArgs.add("-proc:full")
      options.compilerArgs.add("--enable-preview")
      options.compilerArgs.addAll(
        listOf(
          "-Xlint:all",         // enable all warnings
          "-Xlint:-preview",    // reduce warning size for the following warning types
          "-Xlint:-unchecked",
          "-Xlint:-processing",
        )
      )
    }
  }
}

private fun Project.configureShadow() {
  plugins.withId("com.gradleup.shadow") {
    tasks.named("assemble").configure {
      dependsOn(tasks.named("shadowJar"))
    }
    tasks.named<Jar>("jar").configure {
      // we use the shadow jar task, so move the jar into the task's temporary dir to avoid clutter
      destinationDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }
  }
}

private fun Project.applyJavaLauncher() {
  // This must be inside withPlugin("java-base") or else the extensions won't be registered yet
  pluginManager.withPlugin("java-base") {
    val service = extensions.getByType<JavaToolchainService>()
    val extension = extensions.getByType<JavaPluginExtension>()
    val launcher = service.launcherFor(extension.toolchain)
    val javadoc = service.javadocToolFor { this.configureFor(JAVA_CORE_COMPATIBILITY) }
    val latestLauncher = service.launcherFor { this.configureFor(JAVA_CORE_COMPATIBILITY) }

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
      options.source = JAVA_CORE_COMPATIBILITY.toString()
    }
  }
}

private fun Project.configureCheckstyle() {
  tasks.withType<Checkstyle>().configureEach {
    maxErrors = 0
    maxWarnings = 0
    configFile = project.isolated.rootProject.projectDirectory.file("checkstyle.xml").asFile
  }

  extensions.configure<CheckstyleExtension> {
    toolVersion = libs.map { it.findVersion("checkstyleTools").orElseThrow().requiredVersion }.get()
  }
}

private fun Project.configureSpotless() {
  extensions.configure<SpotlessExtension> {
    java {
      licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
    }
  }
}

private fun Project.extendTestImplementationFromCompileOnly() {
  pluginManager.withPlugin("java-base") {
    configurations.named("testImplementation").configure {
      extendsFrom(configurations.named("compileOnly").get())
    }
  }
}

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

fun JavaToolchainSpec.configureFor(version: JavaLanguageVersion) {
  this.languageVersion = version
  this.vendor = JvmVendorSpec.AZUL
}

fun Project.configureJavaVersion() {
  this.configureJavaVersion(JAVA_CORE_COMPATIBILITY)
}

fun Project.configureJavaVersion(version: JavaLanguageVersion) {
  this.extensions.configure<JavaPluginExtension> {
    this.toolchain.configureFor(version)
  }
}
