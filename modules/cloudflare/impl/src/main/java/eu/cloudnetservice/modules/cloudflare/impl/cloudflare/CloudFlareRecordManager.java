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

package eu.cloudnetservice.modules.cloudflare.impl.cloudflare;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.modules.cloudflare.config.CloudflareConfigurationEntry;
import eu.cloudnetservice.modules.cloudflare.impl.dns.DnsRecord;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import kong.unirest.core.ContentType;
import kong.unirest.core.HttpRequestWithBody;
import kong.unirest.core.Unirest;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CloudFlareRecordManager {

  protected static final String CLOUDFLARE_ENDPOINT = "https://api.cloudflare.com/client/v4/";
  protected static final String ZONE_RECORDS_ENDPOINT = CLOUDFLARE_ENDPOINT + "zones/%s/dns_records";
  protected static final String ZONE_RECORDS_MANAGEMENT_ENDPOINT = ZONE_RECORDS_ENDPOINT + "/%s";

  protected static final Type LIST_DNS_RECORD_TYPE = TypeFactory.parameterizedClass(List.class, DnsRecord.class);

  protected static final Logger LOGGER = LoggerFactory.getLogger(CloudFlareRecordManager.class);

  protected final Multimap<UUID, DnsRecordDetail> createdRecords = Multimaps.newMultimap(
    new ConcurrentHashMap<>(),
    ConcurrentHashMap::newKeySet);

  public @NonNull CompletableFuture<List<DnsRecord>> listRecords(@NonNull CloudflareConfigurationEntry configuration) {
    // build the request
    var request = this.prepareRequest(
      String.format("%s?per_page=5000&proxied=false", String.format(ZONE_RECORDS_ENDPOINT, configuration.zoneId())),
      "GET",
      configuration);

    // send the request and handle the response
    return this.sendRequestAndReadResponse(request, null).thenApply(response -> {
      // check if the response is valid
      List<DnsRecord> result = response.readObject("result", LIST_DNS_RECORD_TYPE, null);
      if (result == null || !response.getBoolean("success")) {
        LOGGER.debug("Unable to list records for config {}; response was {}", configuration, response);
        return List.of();
      }

      // return the result
      return result;
    });
  }

  public @NonNull CompletableFuture<DnsRecordDetail> createRecord(
    @NonNull UUID serviceUniqueId,
    @NonNull CloudflareConfigurationEntry configuration,
    @NonNull DnsRecord record
  ) {
    // build the request
    var request = this.prepareRequest(
      String.format(ZONE_RECORDS_ENDPOINT, configuration.zoneId()),
      "POST",
      configuration);

    // send the request and handle the response
    return this.sendRequestAndReadResponse(request, record).thenApply(response -> {
      // check if the response is valid
      var result = response.readDocument("result", null);
      if (result == null || !response.getBoolean("success")) {
        LOGGER.debug("Unable to create record {} for config {}; response was {}", record, configuration, response);
        return null;
      }

      // successfully created the record
      var id = result.getString("id");
      var recordDetail = new DnsRecordDetail(id, record, configuration);

      // register and return the record
      this.createdRecords.put(serviceUniqueId, recordDetail);
      return recordDetail;
    }).exceptionally(ex -> {
      LOGGER.error("Unable to create cloudflare dns record from {} for config {}", record, configuration, ex);
      return null;
    });
  }

  public @NonNull CompletableFuture<DnsRecordDetail> patchRecord(
    @NonNull UUID serviceUniqueId,
    @NonNull DnsRecordDetail oldRecord,
    @NonNull DnsRecord record
  ) {
    // build the request
    var configuration = oldRecord.configurationEntry();
    var request = this.prepareRequest(
      String.format(ZONE_RECORDS_MANAGEMENT_ENDPOINT, configuration.zoneId(), oldRecord.id()),
      "PATCH",
      configuration);

    // send the request and handle the response
    return this.sendRequestAndReadResponse(request, record).thenApply(response -> {
      // check if the response is valid
      var result = response.readDocument("result", null);
      if (result == null || !response.getBoolean("success")) {
        LOGGER.debug(
          "Unable to patch record {} to {} for config {}; response was {}",
          oldRecord.id(),
          record,
          configuration,
          response);
        return null;
      }

      // successfully patched the record
      var id = result.getString("id");
      var recordDetail = new DnsRecordDetail(id, record, configuration);

      // register and return the record
      this.createdRecords.put(serviceUniqueId, recordDetail);
      return recordDetail;
    }).exceptionally(ex -> {
      LOGGER.error(
        "Unable to patch cloudflare dns record {} to {} for config {}",
        oldRecord.id(),
        record,
        configuration,
        ex);
      return null;
    });
  }

  public @NonNull CompletableFuture<Boolean> deleteRecord(@NonNull DnsRecordDetail recordDetail) {
    return this.deleteRecord(recordDetail.configurationEntry(), recordDetail.id());
  }

  public @NonNull CompletableFuture<Boolean> deleteRecord(
    @NonNull CloudflareConfigurationEntry configuration,
    @NonNull String id
  ) {
    // build the request
    var request = this.prepareRequest(
      String.format(ZONE_RECORDS_MANAGEMENT_ENDPOINT, configuration.zoneId(), id),
      "DELETE",
      configuration);

    // send the request and handle the response
    return this.sendRequestAndReadResponse(request).thenApply(response -> {
      // check if the response is valid
      var deletedRecordId = response.readDocument("result").getString("id");
      if (deletedRecordId == null) {
        LOGGER.debug("Unable to delete record {} (configuration: {}); response: {}", id, configuration, response);
        return false;
      }

      // record was deleted successfully
      return true;
    }).exceptionally(ex -> {
      LOGGER.debug("Unable to delete record {} (configuration: {})", id, configuration, ex);
      return false;
    });
  }

  protected @NonNull HttpRequestWithBody prepareRequest(
    @NonNull String endpoint,
    @NonNull String method,
    @NonNull CloudflareConfigurationEntry entry
  ) {
    return Unirest.request(method, endpoint)
      .headers(this.constructHeaders(entry))
      .accept(ContentType.APPLICATION_JSON.getMimeType())
      .contentType(ContentType.APPLICATION_JSON.getMimeType());
  }

  protected @NonNull CompletableFuture<Document> sendRequestAndReadResponse(@NonNull HttpRequestWithBody request) {
    return this.sendRequestAndReadResponse(request, null);
  }

  protected @NonNull CompletableFuture<Document> sendRequestAndReadResponse(
    @NonNull HttpRequestWithBody bodyRequest,
    @NonNull Object body
  ) {
    return this.sendRequestAndReadResponse(
      bodyRequest,
      Document.newJsonDocument().appendTree(body).serializeToString());
  }

  protected @NonNull CompletableFuture<Document> sendRequestAndReadResponse(
    @NonNull HttpRequestWithBody request,
    @Nullable String body
  ) {
    var response = body == null ? request.asStringAsync() : request.body(body).asStringAsync();
    return response.thenApply(res -> DocumentFactory.json().parse(res.getBody()));
  }

  protected @NonNull Map<String, String> constructHeaders(@NonNull CloudflareConfigurationEntry entry) {
    if (entry.authenticationMethod() == CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY) {
      return Map.of("X-Auth-Email", entry.email(), "X-Auth-Key", entry.apiToken());
    } else {
      return Map.of("Authorization", "Bearer " + entry.apiToken());
    }
  }

  public @NonNull Collection<DnsRecordDetail> getAndRemoveRecords(@NonNull UUID serviceUniqueId) {
    return this.createdRecords.removeAll(serviceUniqueId);
  }

  public @NonNull Collection<DnsRecordDetail> createdRecords(@NonNull UUID serviceUniqueId) {
    return this.createdRecords.get(serviceUniqueId);
  }

  public @NonNull Multimap<UUID, DnsRecordDetail> trackedRecords() {
    return this.createdRecords;
  }
}
