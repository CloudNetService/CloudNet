/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.influx;

import com.influxdb.client.InfluxDBClientFactory;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.influx.publish.PublisherRegistry;
import eu.cloudnetservice.modules.influx.publish.defaults.DefaultPublisherRegistry;
import eu.cloudnetservice.modules.influx.publish.publishers.ConnectedNodeInfoPublisher;
import eu.cloudnetservice.modules.influx.publish.publishers.RunningServiceProcessSnapshotPublisher;
import eu.cloudnetservice.node.TickLoop;
import jakarta.inject.Singleton;
import lombok.NonNull;

@Singleton
public final class InfluxModule extends DriverModule {

  @ModuleTask
  public void start(@NonNull ServiceRegistry serviceRegistry, @NonNull TickLoop mainThread) {
    // read the config and connect to influx
    var conf = this.readConfig(InfluxConfiguration.class, () -> new InfluxConfiguration(
      new HostAndPort("http://127.0.0.1", 8086),
      "token",
      "org",
      "bucket",
      30));
    var influxClient = InfluxDBClientFactory.create(
      conf.connectUrl(),
      conf.token().toCharArray(),
      conf.org(),
      conf.bucket());
    // create an influx publisher registry based on that
    var reg = new DefaultPublisherRegistry(influxClient, mainThread);
    serviceRegistry.registerProvider(PublisherRegistry.class, "InfluxPublishers", reg);
    // register all default publishers
    reg
      .registerPublisher(ConnectedNodeInfoPublisher.class)
      .registerPublisher(RunningServiceProcessSnapshotPublisher.class);
    // start the emitting task
    reg.scheduleTask(conf.publishDelaySeconds() * TickLoop.TPS);
  }
}
