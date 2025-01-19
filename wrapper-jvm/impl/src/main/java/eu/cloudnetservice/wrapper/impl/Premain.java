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

package eu.cloudnetservice.wrapper.impl;

import eu.cloudnetservice.wrapper.impl.transform.DefaultClassTransformerRegistry;
import eu.cloudnetservice.wrapper.transform.ClassTransformerRegistry;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class Premain {

  static Instrumentation instrumentation;
  static ClassTransformerRegistry transformerRegistry;

  public static void premain(@Nullable String agentArgs, @NonNull Instrumentation inst) {
    Premain.instrumentation = inst;
    Premain.transformerRegistry = new DefaultClassTransformerRegistry(inst);
  }

  public static void preloadClasses(@NonNull Path file, @NonNull ClassLoader loader) {
    try (var stream = new JarInputStream(Files.newInputStream(file))) {
      JarEntry entry;
      while ((entry = stream.getNextJarEntry()) != null) {
        // only resolve class files
        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
          // canonicalize the class name
          var className = entry.getName().replace('/', '.').replace(".class", "");
          // load the class
          try {
            Class.forName(className, false, loader);
          } catch (ClassNotFoundException ignored) {
            // ignore
          }
        }
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to preload classes in app file", exception);
    }
  }

  public static void invokePremain(@NonNull String premainClass, @NonNull ClassLoader loader) throws Exception {
    if (!premainClass.equals("null")) {
      try {
        var agentClass = Class.forName(premainClass, true, loader);
        // find any possible premain method as defined in:
        // ~ https://docs.oracle.com/en/java/javase/11/docs/api/java.instrument/java/lang/instrument/package-summary.html
        // agentmain(String, Instrumentation)
        var method = methodOrNull(agentClass, "agentmain", String.class, Instrumentation.class);
        if (method != null) {
          invokeAgentMainMethod(method, "", Premain.instrumentation);
          return;
        }
        // agentmain(String)
        method = methodOrNull(agentClass, "agentmain", String.class);
        if (method != null) {
          invokeAgentMainMethod(method, "");
          return;
        }
        // premain(String, Instrumentation)
        method = methodOrNull(agentClass, "premain", String.class, Instrumentation.class);
        if (method != null) {
          invokeAgentMainMethod(method, "", Premain.instrumentation);
          return;
        }
        // premain(String)
        method = methodOrNull(agentClass, "premain", String.class);
        if (method != null) {
          invokeAgentMainMethod(method, "");
          return;
        }
        // the given agent class has no agent main methods - this should never happen
        throw new IllegalArgumentException("Agent Class " + premainClass + " has no agent main methods");
      } catch (ClassNotFoundException ignored) {
        // the agent main class is not available - this should not happen, but we don't care
      }
    }
  }

  private static void invokeAgentMainMethod(@NonNull Method method, Object... args) throws Exception {
    method.setAccessible(true);
    method.invoke(null, args);
  }

  private static @Nullable Method methodOrNull(@NonNull Class<?> source, @NonNull String name, Class<?>... args) {
    try {
      return source.getDeclaredMethod(name, args);
    } catch (NoSuchMethodException exception) {
      return null;
    }
  }
}
