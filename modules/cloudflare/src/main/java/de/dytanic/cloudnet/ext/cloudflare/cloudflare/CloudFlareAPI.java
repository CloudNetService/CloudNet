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

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSRecord;
import de.dytanic.cloudnet.service.ICloudService;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.Unirest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloudFlareAPI implements AutoCloseable {

  protected static final String CLOUDFLARE_ENDPOINT = "https://api.cloudflare.com/client/v4/";
  protected static final String ZONE_RECORDS_ENDPOINT = CLOUDFLARE_ENDPOINT + "zones/%s/dns_records";
  protected static final String ZONE_RECORDS_MANAGEMENT_ENDPOINT = ZONE_RECORDS_ENDPOINT + "/%s";

  protected static final Logger LOGGER = LogManager.logger(CloudFlareAPI.class);

  protected final Multimap<UUID, DnsRecordDetail> createdRecords = Multimaps
    .newSetMultimap(new ConcurrentHashMap<>(), CopyOnWriteArraySet::new);

  @Nullable
  public DnsRecordDetail createRecord(@NotNull UUID serviceUniqueId,
    @NotNull CloudflareConfigurationEntry configuration, @NotNull DNSRecord record) {
    Preconditions.checkNotNull(serviceUniqueId, "serviceUniqueId");
    Preconditions.checkNotNull(configuration, "configuration");
    Preconditions.checkNotNull(record, "record");

    try {
      var connection = this
        .prepareRequest(String.format(ZONE_RECORDS_ENDPOINT, configuration.getZoneId()), "POST", configuration);
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

  public boolean deleteRecord(@NotNull DnsRecordDetail recordDetail) {
    Preconditions.checkNotNull(recordDetail, "recordDetail");
    return this.deleteRecord(recordDetail.configurationEntry(), recordDetail.id());
  }

  @NotNull
  public Collection<DnsRecordDetail> deleteAllRecords(@NotNull ICloudService service) {
    Preconditions.checkNotNull(service, "service");
    return this.deleteAllRecords(service.getServiceId().getUniqueId());
  }

  @NotNull
  public Collection<DnsRecordDetail> deleteAllRecords(@NotNull UUID serviceUniqueId) {
    Preconditions.checkNotNull(serviceUniqueId, "serviceUniqueId");

    return this.createdRecords.removeAll(serviceUniqueId).stream()
      .filter(this::deleteRecord)
      .collect(Collectors.toSet());
  }

  public boolean deleteRecord(@NotNull CloudflareConfigurationEntry configuration, @NotNull String id) {
    Preconditions.checkNotNull(configuration, "configuration");
    Preconditions.checkNotNull(id, "id");

    try {
      var connection = this
        .prepareRequest(String.format(ZONE_RECORDS_MANAGEMENT_ENDPOINT, configuration.getZoneId(), id), "DELETE",
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

  @NotNull
  protected HttpRequestWithBody prepareRequest(@NotNull String endpoint, @NotNull String method,
    @NotNull CloudflareConfigurationEntry entry) throws IOException {
    Preconditions.checkNotNull(endpoint, "endpoint");
    Preconditions.checkNotNull(method, "method");
    Preconditions.checkNotNull(entry, "entry");

    var bodyRequest = Unirest
      .request(method, endpoint)
      .contentType("application/json")
      .accept("application/json");

    if (entry.getAuthenticationMethod() == CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY) {
      bodyRequest.header("X-Auth-Email", entry.getEmail());
      bodyRequest.header("X-Auth-Key", entry.getApiToken());
    } else {
      bodyRequest.header("Authorization", "Bearer " + entry.getApiToken());
    }

    return bodyRequest;
  }

  @NotNull
  protected JsonDocument sendRequestAndReadResponse(@NotNull HttpRequestWithBody bodyRequest) throws IOException {
    Preconditions.checkNotNull(bodyRequest, "bodyRequest");
    return this.sendRequestAndReadResponse(bodyRequest, (String) null);
  }

  @NotNull
  protected JsonDocument sendRequestAndReadResponse(@NotNull HttpRequestWithBody bodyRequest, @NotNull DNSRecord record)
    throws IOException {
    Preconditions.checkNotNull(bodyRequest, "bodyRequest");
    Preconditions.checkNotNull(record, "record");

    return this.sendRequestAndReadResponse(bodyRequest, JsonDocument.newDocument(record).toString());
  }

  @NotNull
  protected JsonDocument sendRequestAndReadResponse(@NotNull HttpRequestWithBody bodyRequest, @Nullable String data) {
    Preconditions.checkNotNull(bodyRequest, "bodyRequest");

    if (data != null) {
      bodyRequest.body(data);
    }

    var response = bodyRequest.asString();
    return JsonDocument.fromJsonString(response.getBody());
  }

  @Override
  public void close() {
    for (var entry : this.createdRecords.entries()) {
      this.deleteRecord(entry.getValue());
    }
  }

  @NotNull
  public Collection<DnsRecordDetail> getCreatedRecords(@NotNull UUID serviceUniqueId) {
    Preconditions.checkNotNull(serviceUniqueId, "serviceUniqueId");
    return this.createdRecords.get(serviceUniqueId);
  }

  @NotNull
  public Collection<DnsRecordDetail> getCreatedRecords() {
    return this.createdRecords.values();
  }

  @NotNull
  public Collection<UUID> getServiceUniqueIds() {
    return this.createdRecords.keys();
  }
}
