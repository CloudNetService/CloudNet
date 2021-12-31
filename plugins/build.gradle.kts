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

subprojects {
  repositories {
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
  }

  tasks.named<Copy>("processResources") {
    filter {
      it
        .replace("{project.build.version}", project.version.toString())
        .replace("{project.perms.build.version}", projects.cloudnetModules.cloudperms.version.toString())
    }
  }
}
