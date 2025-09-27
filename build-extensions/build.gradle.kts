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
  `kotlin-dsl`
  id("java-gradle-plugin")
}

// we are using gradle daemon jvm 25, so this can safely be 21 for potential future compatibility
kotlin.jvmToolchain(21)

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation("org.eclipse.jgit:org.eclipse.jgit:7.3.0.202506031305-r")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("com.google.code.gson:gson:2.13.1")

  implementation("com.diffplug.spotless:spotless-plugin-gradle:" + libs.versions.spotless.get())
  implementation("com.gradleup.shadow:shadow-gradle-plugin:" + libs.versions.shadow.get())
  implementation("eu.cloudnetservice.gradle:juppiter:" + libs.versions.juppiter.get())
}

gradlePlugin {
  plugins {
    register("cloudnet") {
      id = "cloudnet"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetPlugin"
    }
    register("cloudnet-git") {
      id = "cloudnet-git"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.git.CloudNetGitPlugin"
    }
    register("cloudnet-updater") {
      id = "cloudnet-updater"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.updater.CloudNetUpdaterPlugin"
    }
    register("cloudnet-settings") {
      id = "cloudnet-settings"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetSettingsPlugin"
    }
    register("cloudnet-publish") {
      id = "cloudnet-publish"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetPublishPlugin"
    }
    register("cloudnet-java") {
      id = "cloudnet-java"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetJavaPlugin"
    }
    register("cloudnet-java-api") {
      id = "cloudnet-java-api"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetJavaApiPlugin"
    }
    register("cloudnet-modules") {
      id = "cloudnet-modules"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetModulesPlugin"
    }
    register("cloudnet-modules-api") {
      id = "cloudnet-modules-api"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetModulesApiPlugin"
    }
    register("cloudnet-plugins") {
      id = "cloudnet-plugins"
      implementationClass = "eu.cloudnetservice.cloudnet.gradle.plugins.CloudNetPluginsPlugin"
    }
  }
}
