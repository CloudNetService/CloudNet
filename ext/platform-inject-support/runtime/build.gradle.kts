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

repositories {
  maven("https://repo.waterdog.dev/releases/")
  maven("https://repo.loohpjames.com/repository")
  maven("https://repo.md-5.net/repository/releases/")
  maven("https://repo.md-5.net/repository/snapshots/")
  maven("https://repo.opencollab.dev/maven-releases/")
  maven("https://repo.opencollab.dev/maven-snapshots/")
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
  "compileOnly"(libs.bundles.proxyPlatform)
  "compileOnly"(libs.bundles.serverPlatform)
  "compileOnly"(projects.ext.platformInjectSupport.platformInjectApi)
}

tasks.withType<Jar> {
  archiveFileName.set(Files.injectSupport)
}
