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

package eu.cloudnetservice.modules.prometheus.impl;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import org.microhttp.EventLoop;
import org.microhttp.Handler;
import org.microhttp.Header;
import org.microhttp.Response;

public class PrometheusModule extends DriverModule {

  private EventLoop httpEventLoop;

  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
  private void registerHttpExporter(
    @NonNull CompositeMeterRegistry registry,
    @NonNull @Named("module") InjectionLayer<?> layer
  ) throws IOException {
    var prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    prometheusRegistry.config().namingConvention(registry.config().namingConvention());

    var bindingBuilder = layer.injector().createBindingBuilder();
    layer.install(bindingBuilder.bind(PrometheusMeterRegistry.class).toInstance(prometheusRegistry));

    registry.add(prometheusRegistry);

    Handler handler = (_, callback) -> {
      var response = new Response(
        200,
        "OK",
        List.of(new Header("Content-Type", "text/plain")),
        prometheusRegistry.scrape().getBytes());
      callback.accept(response);
    };
    this.httpEventLoop = new EventLoop(handler);
    this.httpEventLoop.start();
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
  private void stopHttpExporter() {
    if (this.httpEventLoop != null) {
      this.httpEventLoop.stop();
    }
  }
}
