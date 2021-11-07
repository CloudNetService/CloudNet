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

object Versions {

  // internal versions
  const val cloudNet = "4.0.0-SNAPSHOT"
  const val cloudNetCodeName = "Blizzard"

  // google libs
  const val gson = "2.8.9"
  const val guava = "31.0.1-jre"

  // testing
  const val junit = "5.8.1"
  const val mockito = "4.0.0"
  const val tyrusClient = "2.0.1"

  // compile time processing
  const val lombok = "1.18.22"
  const val checkstyle = "9.1"

  // console
  const val jansi = "2.4.0"
  const val cloud = "1.6.0-SNAPSHOT"
  const val jline = "3.21.0"

  // databases
  const val h2 = "1.4.197" // do not update, leads to database incompatibility
  const val xodus = "1.3.232"
  const val mongodb = "4.3.3"
  const val mysqlConnector = "8.0.27"
  const val hikariCp = "5.0.0"

  const val asm = "9.2"
  const val slf4j = "1.7.32"
  const val jjwt = "0.11.2"
  const val javers = "6.4.2"
  const val netty = "4.1.70.Final"
  const val unirest = "4.0.0-RC1"

  // platform api versions
  const val velocity = "3.0.1"
  const val nukkitX = "1.0-SNAPSHOT"
  const val sponge = "8.0.0-SNAPSHOT"
  const val waterdogpe = "1.1.4"
  const val spigot = "1.17.1-R0.1-SNAPSHOT"
  const val bungeecord = "1.17-R0.1-SNAPSHOT"

  const val vault = "1.7.1"
  const val adventure = "4.9.3"
  const val annotations = "22.0.0"
  /*
  //Dependencies
    dependencyVaultVersion = '1.7.1'
    dependencyProtocolLibVersion = 'master-SNAPSHOT'
    dependencyNpcLibVersion = 'development-SNAPSHOT'
    dependencyCommonsNetVersion = '3.8.0'
    dependencyJschVersion = '0.1.55'
   */
}
