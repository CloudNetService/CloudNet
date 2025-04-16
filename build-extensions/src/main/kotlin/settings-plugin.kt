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
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

class SettingsPlugin : Plugin<Settings> {
  override fun apply(settings: Settings) {
    settings.gradle.lifecycle.beforeProject {
      plugins.apply(AllProjects::class)
      if (this != this.rootProject) {
        plugins.apply(JavaProjects::class)
      }
    }
  }
}

class JavaProjects : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      // these are top level projects which are configured separately
      if (name == "modules" || name == "plugins" || name == "ext" || name == "launcher") {
        return@run
      }

      // these are the plugins which we need to apply to all projects
      apply(plugin = "signing")
      apply(plugin = "maven-publish")

      // skip further applying to bom - this project is a bit special as we're not allowed to
      // apply the java plugin to it (that's why we need to stop here, but we need to publish
      // at well (that's why we're applying the publish plugin)
      if (name == "bom") {
        return@run
      }

      apply(plugin = "checkstyle")
      apply(plugin = "java-library")
      apply(plugin = "com.diffplug.spotless")

      val libs = rootProject.versionCatalogs.named("libs")

      dependencies {
        // the 'rootProject.libs.' prefix is needed here - see https://github.com/gradle/gradle/issues/16634
        // lombok
        "compileOnly"(libs.findLibrary("lombok").orElseThrow())
        "annotationProcessor"(libs.findLibrary("lombok").orElseThrow())
        // annotations
        "compileOnly"(libs.findLibrary("annotations").orElseThrow())
        // testing
        "testImplementation"(libs.findLibrary("mockito").orElseThrow())
        "testRuntimeOnly"(libs.findLibrary("junitLauncher").orElseThrow())
        "testImplementation"(libs.findBundle("junit").orElseThrow())
        "testImplementation"(libs.findBundle("testContainers").orElseThrow())
      }

      configurations.all {
        // unsure why but every project loves them, and they literally have an import for every letter I type - beware
        exclude("org.checkerframework", "checker-qual")
      }

      tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE"))
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
      }

      tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
          events("started", "passed", "skipped", "failed")
        }

        // allow dynamic agent loading for mockito
        jvmArgs(
          "--enable-preview",
          "-XX:+EnableDynamicAgentLoading",
          "--enable-native-access=ALL-UNNAMED",
          "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
        )

        // always pass down all given system properties
        systemProperties(System.getProperties().mapKeys { it.key.toString() })
      }

      tasks.withType<JavaCompile>().configureEach {
        val javaVersion = if (project.path.contains("api")) JavaVersion.VERSION_17 else JavaVersion.VERSION_24
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()

        options.encoding = "UTF-8"
        options.isIncremental = true

        if (project.path != ":launcher:java8" && project.path != ":launcher:patcher" && !project.path.contains("api")) {
          options.compilerArgs.add("--enable-preview")
          options.compilerArgs.add("-Xlint:-deprecation,-unchecked,-preview")
          options.compilerArgs.add("-proc:full")
        }
      }

      if (project.plugins.hasPlugin(ShadowJavaPlugin::class)) {
        tasks.named("assemble").configure {
          dependsOn(project.tasks.named("shadowJar"))
        }
        tasks.named<Jar>("jar").configure {
          // we use the shadow jar task, so move the jar into the task's temporary dir to avoid clutter
          destinationDirectory = temporaryDir
        }
      }

      tasks.withType<Checkstyle>().configureEach {
        maxErrors = 0
        maxWarnings = 0
        configFile = rootProject.file("checkstyle.xml")
      }

      extensions.configure<CheckstyleExtension> {
        toolVersion = libs.findVersion("checkstyleTools").orElseThrow().requiredVersion
      }

      extensions.configure<SpotlessExtension> {
        java {
          licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
        }
      }

      val java = project.extensions.getByType<JavaPluginExtension>()
      java.withSourcesJar()
      java.withJavadocJar()

      tasks.withType<Javadoc>().configureEach {
        val options = options as? StandardJavadocDocletOptions ?: return@configureEach
        applyDefaultJavadocOptions(options)
      }

      tasks.withType<JavaCompile>().configureEach {
        dependsOn(tasks.withType<ProcessResources>())
      }

      // all these projects are publishing their java artifacts
      configurePublishing("java", true)
    }
  }
}

class AllProjects : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      this.version = Versions.cloudNet
      this.group = "eu.cloudnetservice.cloudnet"
      this.description = "A modern application that can dynamically and easily deliver Minecraft oriented software"

      this.repositories {
        releasesOnly(mavenCentral())
        snapshotsOnly(maven("https://central.sonatype.com/repository/maven-snapshots/"))

        // ensure that we use these repositories for snapshots/releases only (improves lookup times)
        releasesOnly(maven("https://repository.derklaro.dev/releases/"))
        snapshotsOnly(maven("https://repository.derklaro.dev/snapshots/"))

        // must be after sonatype as sponge mirrors sonatype which leads to outdated dependencies
        maven("https://repo.spongepowered.org/maven/")
      }
    }
  }
}
