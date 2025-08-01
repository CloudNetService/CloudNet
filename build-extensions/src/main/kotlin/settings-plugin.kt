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

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByName

fun isJavaConfiguredProject(name: String, path: String): Boolean {
  if (isHelperProject(path)) return false
  return name != "bom"
}

fun isHelperProject(path: String): Boolean {
  val guaranteedHelper =
    path == ":modules" || path == ":plugins" || path == ":ext" || path == ":launcher" || path == ":node" || path == ":driver" || path == ":wrapper-jvm" || path == ":utils" || path == ":ext:platform-inject-support"
  if (guaranteedHelper) return true
  val couldBeHelper = path.startsWith(":modules:") || path.startsWith(":node:") || path.startsWith(":wrapper-jvm:")
  val apiOrImpl = path.endsWith("-api") || path.endsWith("-impl")
  return couldBeHelper && !apiOrImpl
}

object CustomConfigurations {
  const val GLOBAL_JAVADOC_SOURCES = "globalJavadocSources"
  const val GLOBAL_JAVADOC_CLASSPATH = "globalJavadocClasspath"
}

internal fun Provider<VersionCatalog>.library(name: String): Provider<MinimalExternalModuleDependency> {
  return flatMap {
    it.findLibrary(name)
      .orElseThrow { NoSuchElementException("Failed to find library named $name. Valid names: ${it.libraryAliases}") }
  }
}

internal fun Provider<VersionCatalog>.bundle(name: String): Provider<ExternalModuleDependencyBundle> {
  // We can't use flatMap here, because Gradle uses internal magic
  return get().let {
    it.findBundle(name)
      .orElseThrow { NoSuchElementException("Failed to find bundle named $name. Valid names: ${it.bundleAliases}") }
  }
}

internal val Project.libs: Provider<VersionCatalog>
  get() = provider { extensions.getByName<VersionCatalogsExtension>("versionCatalogs").named("libs") }
