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

package eu.cloudnetservice.node.impl.console.util;

import com.google.common.io.CharStreams;
import eu.cloudnetservice.node.impl.console.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeaderReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(HeaderReader.class);

  private HeaderReader() {
    throw new UnsupportedOperationException();
  }

  public static void readAndPrintHeader(@NonNull Console console) {
    var version = HeaderReader.class.getPackage().getImplementationVersion();
    var codename = HeaderReader.class.getPackage().getImplementationTitle();

    try (var reader = new InputStreamReader(
      Objects.requireNonNull(HeaderReader.class.getClassLoader().getResourceAsStream("header.txt")),
      StandardCharsets.UTF_8
    )) {
      for (var line : CharStreams.readLines(reader)) {
        console.forceWriteLine(line.replace("%codename%", codename).replace("%version%", version));
      }
    } catch (IOException exception) {
      LOGGER.error("Exception while reading header", exception);
    }
  }
}
