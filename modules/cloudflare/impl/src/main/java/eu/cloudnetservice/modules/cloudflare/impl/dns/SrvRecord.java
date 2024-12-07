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

package eu.cloudnetservice.modules.cloudflare.impl.dns;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareConfigurationEntry;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareGroupConfiguration;
import lombok.NonNull;

public final class SrvRecord extends DnsRecord {

  public SrvRecord(
    @NonNull String name,
    @NonNull String content,
    @NonNull String service,
    @NonNull String proto,
    @NonNull String secondName,
    int priority,
    int weight,
    int port,
    @NonNull String target
  ) {
    super(
      DnsType.SRV,
      name,
      content,
      1,
      false,
      Document.newJsonDocument()
        .append("service", service)
        .append("proto", proto)
        .append("name", secondName)
        .append("priority", priority)
        .append("weight", weight)
        .append("port", port)
        .append("target", target));
  }

  public static @NonNull SrvRecord forConfiguration(
    @NonNull CloudflareConfigurationEntry entry,
    @NonNull CloudflareGroupConfiguration configuration,
    int port
  ) {
    return new SrvRecord(
      String.format("_minecraft._tcp.%s", entry.domainName()),
      String.format(
        "SRV %s %s %s %s.%s",
        configuration.priority(),
        configuration.weight(),
        port,
        entry.entryName(),
        entry.domainName()),
      "_minecraft",
      "_tcp",
      configuration.sub().equals("@") ? entry.domainName() : configuration.sub(),
      configuration.priority(),
      configuration.weight(),
      port,
      String.format("%s.%s", entry.entryName(), entry.domainName()));
  }
}
