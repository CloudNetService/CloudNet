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

package de.dytanic.cloudnet.driver.util;

import java.util.function.Function;

public class PrefixedMessageMapper<T> {

  private final String prefix;
  private final Function<T, String> messageMapper;
  private final String suffix;

  public PrefixedMessageMapper(Function<T, String> messageMapper) {
    this(null, messageMapper);
  }

  public PrefixedMessageMapper(String prefix, Function<T, String> messageMapper) {
    this(prefix, messageMapper, null);
  }

  public PrefixedMessageMapper(String prefix, Function<T, String> messageMapper, String suffix) {
    this.prefix = prefix;
    this.messageMapper = messageMapper;
    this.suffix = suffix;
  }

  public String getPrefix() {
    return this.prefix;
  }

  public Function<T, String> getMessageMapper() {
    return this.messageMapper;
  }

  public String getSuffix() {
    return this.suffix;
  }
}
