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

dependencies {
  "implementation"(projects.utils.utilsBase)
  "implementation"(projects.driver.driverApi)
  "implementation"(projects.ext.updater)

  "implementation"(libs.gson)
  "implementation"(libs.guava)
  "implementation"(libs.caffeine)
  "implementation"(libs.reflexion)
  "implementation"(libs.bundles.unirest)

  "implementation"(libs.bundles.netty)
  "implementation"(libs.nettyNativeKqueue)
  "implementation"(variantOf(libs.nettyNativeEpoll) { classifier("linux-x86_64") })
  "implementation"(variantOf(libs.nettyNativeEpoll) { classifier("linux-aarch_64") })

  "annotationProcessor"(libs.aerogelAuto)
  "annotationProcessor"(projects.driver.driverAp)
}

tasks.withType<JavaCompile> {
  options.compilerArgs.add("-AaerogelAutoFileName=autoconfigure/driver.aero")
}
