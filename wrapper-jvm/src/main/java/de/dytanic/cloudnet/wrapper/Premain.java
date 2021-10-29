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
import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class Premain {

  private static Instrumentation instrumentation;

  public static void premain(@NotNull String agentArgs, @NotNull Instrumentation inst) {
    Premain.instrumentation = inst;
  }

  public static void invokePremain(@NotNull String premainClass, @NotNull ClassLoader loader) throws Exception {
    if (!premainClass.equals("null")) {
      try {
        Class<?> agentClass = Class.forName(premainClass, true, loader);
        // find any possible premain method as defined in:
        // ~ https://docs.oracle.com/en/java/javase/11/docs/api/java.instrument/java/lang/instrument/package-summary.html
        // agentmain(String, Instrumentation)
        Method method = methodOrNull(agentClass, "agentmain", String.class, Instrumentation.class);
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

  private static void invokeAgentMainMethod(@NotNull Method method, Object... args) throws Exception {
    method.setAccessible(true);
    method.invoke(null, args);
  }

  private static @Nullable Method methodOrNull(@NotNull Class<?> source, @NotNull String name, Class<?>... args) {
    try {
      return source.getDeclaredMethod(name, args);
    } catch (NoSuchMethodException exception) {
      return null;
    }
  }
}
