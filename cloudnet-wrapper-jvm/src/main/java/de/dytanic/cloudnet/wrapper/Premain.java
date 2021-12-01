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

package de.dytanic.cloudnet.wrapper;

import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.jetbrains.annotations.NotNull;

final class Premain {

  static Instrumentation instrumentation;

  public static void premain(String args, Instrumentation instrumentation) {
    Premain.instrumentation = instrumentation;
  }

  public static void loadAllClasses(@NotNull Path file, @NotNull ClassLoader loader) throws Exception {
    try (JarInputStream stream = new JarInputStream(Files.newInputStream(file))) {
      JarEntry entry;
      while ((entry = stream.getNextJarEntry()) != null) {
        // only resolve class files
        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
          // canonicalize the class name
          String className = entry.getName().replace('/', '.').replace(".class", "");
          // load the class
          try {
            Class.forName(className, false, loader);
          } catch (Throwable ignored) {
            // ignore
          }
        }
      }
    }
  }
}
