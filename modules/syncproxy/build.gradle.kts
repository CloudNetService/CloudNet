/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

/*
 * Copyright 2019-2021 CloudNetService team & contributors
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
  id("com.github.johnrengelman.shadow") version Versions.shadow
}

tasks.withType<Jar> {
  archiveFileName.set(Files.syncproxy)
}

dependencies {
  "moduleDependency"(projects.cloudnetModules.bridge)
  "compileOnly"(projects.cloudnetWrapperJvm)
  "compileOnly"(libs.bundles.proxyPlatform)

  "annotationProcessor"(libs.velocity)
  "implementation"(projects.cloudnetExt.adventureHelper)
}

moduleJson {
  author = "CloudNetService"
  main = "eu.cloudnetservice.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule"
  description = "CloudNet extension which serves proxy utils with CloudNet support"
  name = "CloudNet-SyncProxy"
}
