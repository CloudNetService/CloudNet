/*
 * Copyright 2019-present CloudNetService team & contributors
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

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.wrapper.impl.transform.ClassTransformerRegistry;
import eu.cloudnetservice.wrapper.impl.transform.DefaultClassTransformerRegistry;
import eu.cloudnetservice.wrapper.impl.transform.unsafe.UnsafeTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class Premain {

  public static void premain(@Nullable String agentArgs, @NonNull Instrumentation inst) {
    var transformerRegistry = new DefaultClassTransformerRegistry(inst);

    // init and registers the unsafe transformer very early in the process. this is done here
    // as we usually don't allow transformers to be registered so early as they're intended to
    // transform classes brought in by the wrapped application, not by the jdk
    var unsafeTransformerDisabled = Boolean.getBoolean("cloudnet.wrapper.unsafe-transform-disabled");
    if (!unsafeTransformerDisabled) {
      UnsafeTransformer.init(inst);
      transformerRegistry.registerTransformer(new UnsafeTransformer());
    }

    bootstrapWrapper(transformerRegistry);
  }

  private static void bootstrapWrapper(@NonNull ClassTransformerRegistry transformerRegistry) {
    var startInstant = Instant.now();

    // initialize injector & install all autoconfigure bindings
    var bootInjectLayer = InjectionLayer.boot();
    bootInjectLayer.installAutoConfigureBindings(Wrapper.class.getClassLoader(), "driver");
    bootInjectLayer.installAutoConfigureBindings(Wrapper.class.getClassLoader(), "wrapper");

    // initial bindings which we cannot (or it makes no sense to) construct
    var builder = bootInjectLayer.injector().createBindingBuilder();
    bootInjectLayer.install(builder.bind(Instant.class).qualifiedWithName("startInstant").toInstance(startInstant));

    var threadFactory = Thread.ofPlatform()
      .daemon(true)
      .priority(Thread.NORM_PRIORITY)
      .inheritInheritableThreadLocals(true)
      .name("CloudNet-TaskScheduler-Thread-", 0L)
      .factory();
    bootInjectLayer.install(builder
      .bind(ScheduledExecutorService.class)
      .qualifiedWithName("taskScheduler")
      .toInstance(Executors.newScheduledThreadPool(2, threadFactory)));

    // bind the transformer registry here - we *could* provided it by constructing, but we don't
    // want to expose the Instrumentation instance
    bootInjectLayer.install(builder.bind(ClassTransformerRegistry.class).toInstance(transformerRegistry));

    // boot the wrapper
    bootInjectLayer.instance(Wrapper.class);
  }

  public static void invokePremain(@NonNull Instrumentation instrumentation) throws Exception {
    try {
      var agentClassName = System.getProperty("cloudnet.wrapper.launcher-agent-class");
      if (agentClassName == null || agentClassName.isBlank()) {
        return;
      }

      // find any possible premain method as defined in:
      // ~ https://docs.oracle.com/en/java/javase/25/docs/api/java.instrument/java/lang/instrument/package-summary.html
      var agentClass = Class.forName(agentClassName, true, Premain.class.getClassLoader());

      // agentmain(String, Instrumentation)
      var method = agentMethodOrNull(agentClass, String.class, Instrumentation.class);
      if (method != null) {
        invokeAgentMainMethod(method, "", instrumentation);
        return;
      }
      // agentmain(String)
      method = agentMethodOrNull(agentClass, String.class);
      if (method != null) {
        invokeAgentMainMethod(method, "");
        return;
      }
      // the given agent class has no agent main methods - this should never happen
      throw new IllegalArgumentException("Agent Class " + agentClassName + " has no agent main methods");
    } catch (ClassNotFoundException ignored) {
      // the agent main class is not available - this should not happen, but we don't care
    }
  }

  private static void invokeAgentMainMethod(@NonNull Method method, Object... args) throws Exception {
    method.setAccessible(true);
    method.invoke(null, args);
  }

  private static @Nullable Method agentMethodOrNull(@NonNull Class<?> source, Class<?>... args) {
    try {
      return source.getDeclaredMethod("agentmain", args);
    } catch (NoSuchMethodException exception) {
      return null;
    }
  }
}
