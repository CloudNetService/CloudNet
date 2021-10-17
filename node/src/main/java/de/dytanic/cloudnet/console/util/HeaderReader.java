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

package de.dytanic.cloudnet.console.util;

import com.google.common.io.CharStreams;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.console.IConsole;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class HeaderReader {

  private static final Logger LOGGER = LogManager.getLogger(HeaderReader.class);

  private HeaderReader() {
    throw new UnsupportedOperationException();
  }

  public static void readAndPrintHeader(@NotNull IConsole console) {
    String version = HeaderReader.class.getPackage().getImplementationVersion();
    String codename = HeaderReader.class.getPackage().getImplementationTitle();

    try (Reader reader = new InputStreamReader(
      Objects.requireNonNull(HeaderReader.class.getClassLoader().getResourceAsStream("header.txt")),
      StandardCharsets.UTF_8
    )) {
      for (String line : CharStreams.toString(reader).split(System.lineSeparator())) {
        console.forceWriteLine(line.replace("%codename%", codename).replace("%version%", version));
      }
    } catch (IOException exception) {
      LOGGER.severe("Exception while reading header", exception);
    }
  }
}
