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

package eu.cloudnetservice.node.impl.boot;

import com.google.common.collect.Lists;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.util.Qualifiers;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.node.impl.Node;
import io.leangen.geantyref.TypeFactory;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Bootstrap {

  private Bootstrap() {
    throw new UnsupportedOperationException();
  }

  public static void main(@NonNull String[] args) throws Throwable {
    var startInstant = Instant.now();

    // initialize injector & install all autoconfigure bindings
    var bootInjectLayer = InjectionLayer.boot();
    bootInjectLayer.installAutoConfigureBindings(Bootstrap.class.getClassLoader(), "node");
    bootInjectLayer.installAutoConfigureBindings(Bootstrap.class.getClassLoader(), "driver");

    ServiceRegistry.registry().discoverServices(Node.class);

    var rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    // initial bindings which we cannot (or it makes no sense to) construct
    bootInjectLayer.install(BindingBuilder.create()
      .bind(Element.forType(org.slf4j.Logger.class).requireAnnotation(Qualifiers.named("root")))
      .toInstance(rootLogger));
    bootInjectLayer.install(BindingBuilder.create()
      .bind(Element.forType(Instant.class).requireAnnotation(Qualifiers.named("startInstant")))
      .toInstance(startInstant));
    bootInjectLayer.install(BindingBuilder.create()
      .bind(Element.forType(ScheduledExecutorService.class).requireAnnotation(Qualifiers.named("taskScheduler")))
      .toInstance(Executors.newScheduledThreadPool(2)));

    // console arguments
    var type = TypeFactory.parameterizedClass(List.class, String.class);
    bootInjectLayer.install(BindingBuilder.create()
      .bind(Element.forType(type).requireAnnotation(Qualifiers.named("consoleArgs")))
      .toInstance(Lists.newArrayList(args)));

    // boot CloudNet
    bootInjectLayer.instance(Node.class);
  }
}
