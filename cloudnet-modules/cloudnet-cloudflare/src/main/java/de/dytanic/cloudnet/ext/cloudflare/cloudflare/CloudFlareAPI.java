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
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.gson.GsonUtil;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSRecord;
import de.dytanic.cloudnet.service.ICloudService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloudFlareAPI implements AutoCloseable {

  protected static final String CLOUDFLARE_ENDPOINT = "https://api.cloudflare.com/client/v4/";
  protected static final String ZONE_RECORDS_ENDPOINT = CLOUDFLARE_ENDPOINT + "zones/%s/dns_records";
  protected static final String ZONE_RECORDS_MANAGEMENT_ENDPOINT = ZONE_RECORDS_ENDPOINT + "/%s";

  protected final Multimap<UUID, DnsRecordDetail> createdRecords = Multimaps
    .newSetMultimap(new ConcurrentHashMap<>(), CopyOnWriteArraySet::new);

  @Nullable
  public DnsRecordDetail createRecord(@NotNull UUID serviceUniqueId,
    @NotNull CloudflareConfigurationEntry configuration, @NotNull DNSRecord record) {
    Preconditions.checkNotNull(serviceUniqueId, "serviceUniqueId");
    Preconditions.checkNotNull(configuration, "configuration");
    Preconditions.checkNotNull(record, "record");

    try {
      HttpURLConnection connection = this
        .prepareConnection(String.format(ZONE_RECORDS_ENDPOINT, configuration.getZoneId()), "POST", configuration);
      JsonDocument result = this.sendRequestAndReadResponse(connection, record);

      JsonDocument content = result.getDocument("result");
      if (result.getBoolean("success") && content != null) {
        String id = content.getString("id");
        if (id != null) {
          CloudNet.getInstance().getLogger().debug(
            "Successfully created record with id " + id + " based on " + record + " (configuration: " + configuration
              + ")");

          DnsRecordDetail detail = new DnsRecordDetail(id, record, configuration);
          this.createdRecords.put(serviceUniqueId, detail);
          return detail;
        }
      } else {
        CloudNet.getInstance().getLogger().debug("Unable to create cloudflare record, response was: " + result);
      }
    } catch (IOException exception) {
      CloudNet.getInstance().getLogger().fatal(
        "Error while creating cloudflare record for configuration " + configuration + " (record: " + record + ")",
        exception
      );
    }

    return null;
  }

  public boolean deleteRecord(@NotNull DnsRecordDetail recordDetail) {
    Preconditions.checkNotNull(recordDetail, "recordDetail");
    return this.deleteRecord(recordDetail.getConfigurationEntry(), recordDetail.getId());
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
      HttpURLConnection connection = this
        .prepareConnection(String.format(ZONE_RECORDS_MANAGEMENT_ENDPOINT, configuration.getZoneId(), id), "DELETE",
          configuration);
      JsonDocument result = this.sendRequestAndReadResponse(connection);

      JsonDocument content = result.getDocument("result");
      if (content != null && content.contains("id")) {
        CloudNet.getInstance().getLogger()
          .debug("Successfully deleted record " + id + " for configuration " + configuration);
        return true;
      }

      CloudNet.getInstance().getLogger().debug("Unable to delete record " + id + ", response: " + result);
    } catch (IOException exception) {
      CloudNet.getInstance().getLogger()
        .fatal("Error while deleting dns record for configuration " + configuration, exception);
    }

    return false;
  }

  @NotNull
  protected HttpURLConnection prepareConnection(@NotNull String endpoint, @NotNull String method,
    @NotNull CloudflareConfigurationEntry entry) throws IOException {
    Preconditions.checkNotNull(endpoint, "endpoint");
    Preconditions.checkNotNull(method, "method");
    Preconditions.checkNotNull(entry, "entry");

    HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
    connection.setConnectTimeout(5000);
    connection.setReadTimeout(5000);
    connection.setUseCaches(false);
    connection.setDoOutput(true);
    connection.setRequestMethod(method);

    connection.setRequestProperty("Accept", "application/json");
    connection.setRequestProperty("Content-Type", "application/json");

    if (entry.getAuthenticationMethod() == CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY) {
      connection.setRequestProperty("X-Auth-Email", entry.getEmail());
      connection.setRequestProperty("X-Auth-Key", entry.getApiToken());
    } else {
      connection.setRequestProperty("Authorization", "Bearer " + entry.getApiToken());
    }

    return connection;
  }

  @NotNull
  protected JsonDocument sendRequestAndReadResponse(@NotNull HttpURLConnection connection) throws IOException {
    Preconditions.checkNotNull(connection, "connection");
    return this.sendRequestAndReadResponse(connection, (String) null);
  }

  @NotNull
  protected JsonDocument sendRequestAndReadResponse(@NotNull HttpURLConnection connection, @NotNull DNSRecord record)
    throws IOException {
    Preconditions.checkNotNull(connection, "connection");
    Preconditions.checkNotNull(record, "record");

    return this.sendRequestAndReadResponse(connection, GsonUtil.GSON.toJson(record));
  }

  @NotNull
  protected JsonDocument sendRequestAndReadResponse(@NotNull HttpURLConnection connection, @Nullable String data)
    throws IOException {
    Preconditions.checkNotNull(connection, "connection");

    connection.connect();

    if (data != null) {
      try (OutputStream outputStream = connection.getOutputStream()) {
        outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      }
    }

    if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
      return JsonDocument.newDocument(connection.getInputStream());
    } else {
      return JsonDocument.newDocument(connection.getErrorStream());
    }
  }

  @Override
  public void close() {
    for (Map.Entry<UUID, DnsRecordDetail> entry : this.createdRecords.entries()) {
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
