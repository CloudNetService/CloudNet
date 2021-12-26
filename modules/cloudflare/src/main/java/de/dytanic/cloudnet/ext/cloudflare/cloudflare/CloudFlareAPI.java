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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSRecord;
import de.dytanic.cloudnet.service.CloudService;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.Unirest;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class CloudFlareAPI implements AutoCloseable {

  protected static final String CLOUDFLARE_ENDPOINT = "https://api.cloudflare.com/client/v4/";
  protected static final String ZONE_RECORDS_ENDPOINT = CLOUDFLARE_ENDPOINT + "zones/%s/dns_records";
  protected static final String ZONE_RECORDS_MANAGEMENT_ENDPOINT = ZONE_RECORDS_ENDPOINT + "/%s";

  protected static final Logger LOGGER = LogManager.logger(CloudFlareAPI.class);

  protected final Multimap<UUID, DnsRecordDetail> createdRecords = Multimaps
    .newSetMultimap(new ConcurrentHashMap<>(), CopyOnWriteArraySet::new);

  public @Nullable DnsRecordDetail createRecord(
    @NonNull UUID serviceUniqueId,
    @NonNull CloudflareConfigurationEntry configuration,
    @NonNull DNSRecord record
  ) {
    try {
      var connection = this
        .prepareRequest(String.format(ZONE_RECORDS_ENDPOINT, configuration.zoneId()), "POST", configuration);
      var result = this.sendRequestAndReadResponse(connection, record);

      var content = result.getDocument("result");
      if (result.getBoolean("success")) {
        var id = content.getString("id");
        if (id != null) {
          LOGGER.fine(
            "Successfully created record with id " + id + " based on " + record + " (configuration: " + configuration
              + ")");

          var detail = new DnsRecordDetail(id, record, configuration);
          this.createdRecords.put(serviceUniqueId, detail);
          return detail;
        }
      } else {
        LOGGER.fine("Unable to create cloudflare record, response was: " + result);
      }
    } catch (IOException exception) {
      LOGGER.severe(
        "Error while creating cloudflare record for configuration " + configuration + " (record: " + record + ")",
        exception
      );
    }

    return null;
  }

  public boolean deleteRecord(@NonNull DnsRecordDetail recordDetail) {
    return this.deleteRecord(recordDetail.configurationEntry(), recordDetail.id());
  }

  public @NonNull Collection<DnsRecordDetail> deleteAllRecords(@NonNull CloudService service) {
    return this.deleteAllRecords(service.serviceId().uniqueId());
  }

  public @NonNull Collection<DnsRecordDetail> deleteAllRecords(@NonNull UUID serviceUniqueId) {
    return this.createdRecords.removeAll(serviceUniqueId).stream()
      .filter(this::deleteRecord)
      .collect(Collectors.toSet());
  }

  public boolean deleteRecord(@NonNull CloudflareConfigurationEntry configuration, @NonNull String id) {
    try {
      var connection = this
        .prepareRequest(String.format(ZONE_RECORDS_MANAGEMENT_ENDPOINT, configuration.zoneId(), id), "DELETE",
          configuration);
      var result = this.sendRequestAndReadResponse(connection);

      var content = result.getDocument("result");
      if (content.contains("id")) {
        LOGGER.fine("Successfully deleted record " + id + " for configuration " + configuration);
        return true;
      }

      LOGGER.fine("Unable to delete record " + id + ", response: " + result);
    } catch (IOException exception) {
      LOGGER.severe("Error while deleting dns record for configuration " + configuration, exception);
    }

    return false;
  }

  protected @NonNull HttpRequestWithBody prepareRequest(
    @NonNull String endpoint,
    @NonNull String method,
    @NonNull CloudflareConfigurationEntry entry
  ) throws IOException {
    var bodyRequest = Unirest
      .request(method, endpoint)
      .contentType("application/json")
      .accept("application/json");

    if (entry.authenticationMethod() == CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY) {
      bodyRequest.header("X-Auth-Email", entry.email());
      bodyRequest.header("X-Auth-Key", entry.apiToken());
    } else {
      bodyRequest.header("Authorization", "Bearer " + entry.apiToken());
    }

    return bodyRequest;
  }

  protected @NonNull JsonDocument sendRequestAndReadResponse(@NonNull HttpRequestWithBody bodyRequest)
    throws IOException {
    return this.sendRequestAndReadResponse(bodyRequest, (String) null);
  }

  protected @NonNull JsonDocument sendRequestAndReadResponse(
    @NonNull HttpRequestWithBody bodyRequest,
    @NonNull DNSRecord record
  ) throws IOException {
    return this.sendRequestAndReadResponse(bodyRequest, JsonDocument.newDocument(record).toString());
  }

  protected @NonNull JsonDocument sendRequestAndReadResponse(
    @NonNull HttpRequestWithBody bodyRequest,
    @Nullable String data
  ) {
    var response = data == null ? bodyRequest.asString() : bodyRequest.body(data).asString();
    return JsonDocument.fromJsonString(response.getBody());
  }

  @Override
  public void close() {
    for (var entry : this.createdRecords.entries()) {
      this.deleteRecord(entry.getValue());
    }
  }

  public @NonNull Collection<DnsRecordDetail> createdRecords(@NonNull UUID serviceUniqueId) {
    return this.createdRecords.get(serviceUniqueId);
  }

  public @NonNull Collection<DnsRecordDetail> createdRecords() {
    return this.createdRecords.values();
  }

  public @NonNull Collection<UUID> serviceUniqueIds() {
    return this.createdRecords.keys();
  }
}
