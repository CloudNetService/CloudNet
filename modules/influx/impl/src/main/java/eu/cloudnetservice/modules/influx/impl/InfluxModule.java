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

package eu.cloudnetservice.modules.influx.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.exceptions.InfluxException;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.influx.InfluxConfiguration;
import eu.cloudnetservice.modules.influx.publish.Publisher;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class InfluxModule extends DriverModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(InfluxModule.class);

  private Future<?> publisherFuture;
  private InfluxDBClient influxClient;

  @ModuleTask(order = 127)
  public void initAutoServices(@NonNull ServiceRegistry serviceRegistry) {
    serviceRegistry.discoverServices(InfluxModule.class);
  }

  @ModuleTask
  public void start(
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull @Named("taskScheduler") ScheduledExecutorService executorService
  ) {
    // read the config and connect to influx
    var config = this.readConfig(
      InfluxConfiguration.class,
      () -> new InfluxConfiguration(
        new HostAndPort("http://127.0.0.1", 8086),
        "token",
        "org",
        "bucket",
        30),
      DocumentFactory.json());
    this.influxClient = InfluxDBClientFactory.create(
      config.connectUrl(),
      config.token().toCharArray(),
      config.org(),
      config.bucket());
    var writeApi = this.influxClient.getWriteApiBlocking();
    // start the emitting task
    this.publisherFuture = executorService.scheduleWithFixedDelay(() ->
      serviceRegistry.registrations(Publisher.class).forEach(registration -> {
        var publisher = registration.serviceInstance();
        // try to create the point
        var points = publisher.createPoints();
        if (!points.isEmpty()) {
          for (var point : points) {
            // stop writing if one write fails
            try {
              writeApi.writePoint(point);
            } catch (InfluxException exception) {
              LOGGER.warn(
                "Unable to write point into influx db, possibly the config is invalid? {}",
                exception.getMessage());
              break;
            }
          }
        }
      }), 0, config.publishDelaySeconds(), TimeUnit.SECONDS);
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
  public void stop() {
    if (this.publisherFuture != null) {
      this.publisherFuture.cancel(true);
      this.publisherFuture = null;
    }

    this.influxClient.close();
  }
}
