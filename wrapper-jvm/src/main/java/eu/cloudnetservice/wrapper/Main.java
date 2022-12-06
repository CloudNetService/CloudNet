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

package eu.cloudnetservice.wrapper;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import dev.derklaro.aerogel.Bindings;
import dev.derklaro.aerogel.Element;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.wrapper.transform.TransformerRegistry;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;

public final class Main {

  private Main() {
    throw new UnsupportedOperationException();
  }

  public static void main(@NonNull String... args) throws Throwable {
    var startInstant = Instant.now();

    // initialize injector & install all autoconfigure bindings
    var bootInjectLayer = InjectionLayer.boot();
    bootInjectLayer.installAutoConfigureBindings(Main.class.getClassLoader(), "driver");
    bootInjectLayer.installAutoConfigureBindings(Main.class.getClassLoader(), "wrapper");

    // initial bindings which we cannot (or it makes no sense to) construct
    bootInjectLayer.install(Bindings.fixed(Element.forType(Instant.class).requireName("startInstant"), startInstant));
    bootInjectLayer.install(Bindings.fixed(Element.forType(Logger.class).requireName("root"), LogManager.rootLogger()));

    // bind the transformer registry here - we *could* provided it by constructing, but we don't
    // want to expose the Instrumentation instance
    bootInjectLayer.install(Bindings.fixed(Element.forType(TransformerRegistry.class), Premain.transformerRegistry));

    // console arguments
    var type = TypeToken.getParameterized(List.class, String.class).getType();
    bootInjectLayer.install(Bindings.fixed(Element.forType(type).requireName("consoleArgs"), Lists.newArrayList(args)));

    // boot the wrapper
    bootInjectLayer.instance(Wrapper.class);
  }
}
