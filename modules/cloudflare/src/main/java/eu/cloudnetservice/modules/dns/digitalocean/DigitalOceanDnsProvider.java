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

package eu.cloudnetservice.modules.dns.digitalocean;

import eu.cloudnetservice.driver.document.property.DocProperty;
import eu.cloudnetservice.modules.dns.provider.DnsProvider;
import eu.cloudnetservice.modules.dns.provider.DnsProviderZoneConfig;
import eu.cloudnetservice.modules.dns.provider.DnsZoneProvider;
import kong.unirest.core.Unirest;
import lombok.NonNull;

public final class DigitalOceanDnsProvider implements DnsProvider {

  private static final String API_BASE_URL = "https://api.digitalocean.com/v2";

  private static final DocProperty<String> API_KEY_PROPERTY = DocProperty.property("apiKey", String.class);
  private static final DocProperty<String> DOMAIN_NAME_PROPERTY = DocProperty.property("domainName", String.class);

  @Override
  public @NonNull DnsZoneProvider zoneProvider(@NonNull DnsProviderZoneConfig zoneConfig) {
    var apiKey = zoneConfig.readPropertyOrThrow(API_KEY_PROPERTY, () -> new IllegalStateException("api key not set"));
    var domainName = zoneConfig.readPropertyOrThrow(
      DOMAIN_NAME_PROPERTY,
      () -> new IllegalStateException("domain name not set"));

    var unirestInstance = Unirest.spawnInstance();
    var unirestInstanceConfig = unirestInstance.config();
    unirestInstanceConfig.defaultBaseUrl(API_BASE_URL);
    unirestInstanceConfig.connectTimeout((int) zoneConfig.apiConnectTimeout().toMillis());
    unirestInstanceConfig.requestTimeout((int) zoneConfig.apiRequestTimeout().toMillis());
    unirestInstanceConfig.setDefaultHeader("Authorization", "Bearer " + apiKey);

    return new DigitalOceanDnsZoneProvider(domainName, unirestInstance);
  }

  @Override
  public @NonNull String name() {
    return "digitalocean";
  }
}
