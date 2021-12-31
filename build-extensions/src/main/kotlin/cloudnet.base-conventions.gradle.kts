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

import net.kyori.indra.IndraExtension

plugins {
  id("net.kyori.indra.publishing")
}

extensions.configure<IndraExtension> {
  apache2License()

  github("CloudNetService", "CloudNet-v3") {
    ci(true)
    scm(true)
    issues(true)
  }

  publishReleasesTo("releases", "https://repo.cloudnetservice.eu/repository/releases/")
  publishSnapshotsTo("snapshot", "https://repo.cloudnetservice.eu/repository/snapshots/")
}
