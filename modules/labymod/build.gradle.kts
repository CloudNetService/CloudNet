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

tasks.withType<Jar> {
  archiveFileName.set(Files.labymod)
}

dependencies {
  "moduleDependency"(projects.cloudnetModules.bridge)
  "compileOnly"(projects.cloudnetWrapperJvm)
  "compileOnly"(libs.bundles.proxyPlatform)

  "annotationProcessor"(libs.velocity)
}

moduleJson {
  name = "CloudNet-LabyMod"
  author = "CloudNetService"
  main = "eu.cloudnetservice.modules.labymod.node.CloudNetLabyModModule"
  description = "This module adds support for the LabyMod Discord RPC Protocol and the ingame messages when a player plays a gamemode"
}

configure<net.kyori.blossom.BlossomExtension> {
  replaceToken("{project.build.version}", project.version)
}
