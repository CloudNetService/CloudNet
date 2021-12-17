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

package de.dytanic.cloudnet.ext.cloudflare.dns;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareGroupConfiguration;

/**
 * A representation of an SRV DNS record
 */
public class SRVRecord extends DNSRecord {

  public SRVRecord(String name, String content, String service, String proto, String secondName,
    int priority, int weight, int port, String target) {
    super(
      DNSType.SRV.name(),
      name,
      content,
      1,
      false,
      JsonDocument.newDocument()
        .append("service", service)
        .append("proto", proto)
        .append("name", secondName)
        .append("priority", priority)
        .append("weight", weight)
        .append("port", port)
        .append("target", target)
    );
  }

  public static SRVRecord forConfiguration(CloudflareConfigurationEntry entry,
    CloudflareGroupConfiguration configuration, int port) {
    return new SRVRecord(
      String.format("_minecraft._tcp.%s", entry.domainName()),
      String.format(
        "SRV %s %s %s %s.%s",
        configuration.priority(),
        configuration.weight(),
        port,
        CloudNet.instance().getConfig().identity().uniqueId(),
        entry.domainName()
      ),
      "_minecraft",
      "_tcp",
      configuration.sub().equals("@") ? entry.domainName() : configuration.sub(),
      configuration.priority(),
      configuration.weight(),
      port,
      CloudNet.instance().getConfig().identity().uniqueId() + "." + entry.domainName()
    );
  }
}
