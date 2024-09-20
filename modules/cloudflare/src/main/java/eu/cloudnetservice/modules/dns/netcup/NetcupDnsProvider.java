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

package eu.cloudnetservice.modules.dns.netcup;

import eu.cloudnetservice.driver.document.property.DocProperty;
import eu.cloudnetservice.modules.dns.provider.DnsProvider;
import eu.cloudnetservice.modules.dns.provider.DnsProviderZoneConfig;
import eu.cloudnetservice.modules.dns.provider.DnsZoneProvider;
import kong.unirest.core.Unirest;
import lombok.NonNull;

public final class NetcupDnsProvider implements DnsProvider {

  private static final String API_BASE_URL = "https://ccp.netcup.net/run/webservice/servers/endpoint.php?JSON";

  private static final DocProperty<String> API_KEY_PROPERTY = DocProperty.property("apiKey", String.class);
  private static final DocProperty<String> API_PWD_PROPERTY = DocProperty.property("apiPassword", String.class);
  private static final DocProperty<String> DOMAIN_NAME_PROPERTY = DocProperty.property("domainName", String.class);
  private static final DocProperty<String> CUSTOMER_ID_PROPERTY = DocProperty.property("customerId", String.class);

  @Override
  public @NonNull DnsZoneProvider zoneProvider(@NonNull DnsProviderZoneConfig zoneConfig) {
    var apiKey = zoneConfig.readPropertyOrThrow(API_KEY_PROPERTY, () -> new IllegalStateException("api key not set"));
    var apiPwd = zoneConfig.readPropertyOrThrow(API_PWD_PROPERTY, () -> new IllegalStateException("api pwd not set"));
    var domainName = zoneConfig.readPropertyOrThrow(
      DOMAIN_NAME_PROPERTY,
      () -> new IllegalStateException("domain name not set"));
    var customerId = zoneConfig.readPropertyOrThrow(
      CUSTOMER_ID_PROPERTY,
      () -> new IllegalStateException("customer id not set"));

    var unirestInstance = Unirest.spawnInstance();
    var unirestInstanceConfig = unirestInstance.config();
    unirestInstanceConfig.defaultBaseUrl(API_BASE_URL);

    var requestSender = new NetcupSoapRequestSender(customerId, apiKey, apiPwd, unirestInstance);
    return new NetcupDnsZoneProvider(domainName, requestSender);
  }

  @Override
  public @NonNull String name() {
    return "netcup";
  }
}
