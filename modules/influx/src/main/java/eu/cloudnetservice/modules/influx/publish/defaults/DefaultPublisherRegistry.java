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

package eu.cloudnetservice.modules.influx.publish.defaults;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.exceptions.InfluxException;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.influx.publish.Publisher;
import eu.cloudnetservice.modules.influx.publish.PublisherRegistry;
import eu.cloudnetservice.node.TickLoop;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import lombok.NonNull;

public class DefaultPublisherRegistry implements PublisherRegistry {

  private static final Logger LOGGER = LogManager.logger(DefaultPublisherRegistry.class);

  private final TickLoop mainThread;
  private final WriteApiBlocking writeApi;
  private final InfluxDBClient influxClient;
  private final List<Publisher> publishers = new LinkedList<>();

  private Future<?> publishFuture;

  public DefaultPublisherRegistry(@NonNull InfluxDBClient influxClient, @NonNull TickLoop tickLoop) {
    this.mainThread = tickLoop;
    this.influxClient = influxClient;
    this.writeApi = influxClient.getWriteApiBlocking();
  }

  @Override
  public @NonNull PublisherRegistry registerPublisher(@NonNull Class<? extends Publisher> publisher) {
    var injectionLayer = InjectionLayer.findLayerOf(publisher);
    return this.registerPublisher(injectionLayer.instance(publisher));
  }

  @Override
  public @NonNull PublisherRegistry registerPublisher(@NonNull Publisher publisher) {
    this.publishers.add(publisher);
    return this;
  }

  @Override
  public @NonNull PublisherRegistry unregisterPublisher(@NonNull Publisher publisher) {
    this.publishers.remove(publisher);
    return this;
  }

  @Override
  public @NonNull PublisherRegistry unregisterPublishers(@NonNull ClassLoader loader) {
    this.publishers.removeIf(publisher -> publisher.getClass().getClassLoader().equals(loader));
    return this;
  }

  @Override
  public @NonNull Collection<Publisher> registeredPublishers() {
    return List.copyOf(this.publishers);
  }

  @Override
  public void publishData() {
    this.publishers.forEach(publisher -> {
      // try to create the point
      var points = publisher.createPoints();
      if (!points.isEmpty()) {
        for (var point : points) {
          // stop writing if one write fails
          try {
            this.writeApi.writePoint(point);
          } catch (InfluxException exception) {
            LOGGER.warning(
              "Unable to write point into influx db, possibly the config is invalid? %s",
              null,
              exception.getMessage());
            break;
          }
        }
      }
    });
  }

  @Override
  public void scheduleTask(int delayTicks) {
    this.publishFuture = this.mainThread.scheduleTask(() -> {
      this.publishData();
      return null;
    }, delayTicks);
  }

  @Override
  public void close() {
    // stop executing of the publishing task if started
    if (this.publishFuture != null) {
      this.publishFuture.cancel(true);
      this.publishFuture = null;
    }
    this.influxClient.close();
  }
}
