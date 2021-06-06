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

package de.dytanic.cloudnet.ext.cloudflare.cloudflare;

import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSRecord;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class DnsRecordDetail {

  private final String id;
  private final DNSRecord dnsRecord;
  private final CloudflareConfigurationEntry configurationEntry;

  public DnsRecordDetail(String id, DNSRecord dnsRecord, CloudflareConfigurationEntry configurationEntry) {
    this.id = id;
    this.dnsRecord = dnsRecord;
    this.configurationEntry = configurationEntry;
  }

  public String getId() {
    return this.id;
  }

  public DNSRecord getDnsRecord() {
    return this.dnsRecord;
  }

  public CloudflareConfigurationEntry getConfigurationEntry() {
    return this.configurationEntry;
  }
}
