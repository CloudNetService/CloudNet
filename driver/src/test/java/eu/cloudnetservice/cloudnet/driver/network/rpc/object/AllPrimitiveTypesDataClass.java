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

package eu.cloudnetservice.cloudnet.driver.network.rpc.object;

public record AllPrimitiveTypesDataClass(
  byte b,
  short s,
  int i,
  long l,
  float f,
  double d,
  char c,
  String string,
  boolean bol
) {

  public AllPrimitiveTypesDataClass() {
    this((byte) 1, (short) 2, 3, 4L, 5F, 6D, '/', "Hello, World!", true);
  }
}
