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
  "api"(libs.vavr)
  "implementation"(libs.guava)
  // todo(derklaro): well this dependency is here now but i'm not really happy with that - util classes
  //                 should not be responsible for logging, any they should especially not just log
  //                 all exceptions instead of passing them to the caller (see primarily FileUtil)
  "implementation"(libs.slf4jApi)
}
