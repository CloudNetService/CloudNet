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

package de.dytanic.cloudnet.ext.cloudflare;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.gson.GsonUtil;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSRecord;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus;

@Deprecated
@ApiStatus.ScheduledForRemoval
public final class CloudflareAPI implements AutoCloseable {

  private static final String CLOUDFLARE_API_V1 = "https://api.cloudflare.com/client/v4/";
  private static final String CLOUDFLARE_STORE_DOCUMENT = "cloudflare_store";

  private static final Type MAP_STRING_DOCUMENT = new TypeToken<Map<String, Pair<String, JsonDocument>>>() {
  }.getType();

  private static CloudflareAPI instance;

  private final Database database;

  private final Map<String, Pair<String, JsonDocument>> createdRecords = new ConcurrentHashMap<>();

  protected CloudflareAPI(Database database) {
    instance = this;

    this.database = database;

    this.read();
  }

  public static CloudflareAPI getInstance() {
    return CloudflareAPI.instance;
  }

  public Pair<Integer, JsonDocument> createRecord(String serviceName, String email,
    CloudflareConfigurationEntry.AuthenticationMethod authenticationMethod, String apiKey, String zoneId,
    DNSRecord dnsRecord) {
    Preconditions.checkNotNull(zoneId);
    Preconditions.checkNotNull(dnsRecord);

    try {
      HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(
        CLOUDFLARE_API_V1 + "zones/" + zoneId + "/dns_records").openConnection();
      httpURLConnection.setRequestMethod("POST");

      return this
        .getJsonResponse(httpURLConnection, email, authenticationMethod, apiKey, dnsRecord, serviceName, zoneId);
    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return null;
  }

  public Pair<Integer, JsonDocument> updateRecord(String serviceName, String email,
    CloudflareConfigurationEntry.AuthenticationMethod authenticationMethod, String apiKey, String zoneId,
    String recordId, DNSRecord dnsRecord) {
    Preconditions.checkNotNull(zoneId);
    Preconditions.checkNotNull(recordId);
    Preconditions.checkNotNull(dnsRecord);

    try {
      HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(
        CLOUDFLARE_API_V1 + "zones/" + zoneId + "/dns_records/" + recordId).openConnection();
      httpURLConnection.setRequestMethod("PUT");

      return this
        .getJsonResponse(httpURLConnection, email, authenticationMethod, apiKey, dnsRecord, serviceName, zoneId);
    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return null;
  }

  private Pair<Integer, JsonDocument> getJsonResponse(HttpURLConnection httpURLConnection, String email,
    CloudflareConfigurationEntry.AuthenticationMethod authenticationMethod, String apiKey, DNSRecord dnsRecord,
    String serviceName, String zoneId) throws IOException {
    httpURLConnection.setDoOutput(true);

    this.initRequestProperties(httpURLConnection, email, authenticationMethod, apiKey);
    httpURLConnection.connect();

    try (DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream())) {
      dataOutputStream.writeBytes(GsonUtil.GSON.toJson(dnsRecord));
      dataOutputStream.flush();
    }

    int statusCode = httpURLConnection.getResponseCode();
    JsonDocument document = JsonDocument.newDocument(this.readResponse(httpURLConnection));
    httpURLConnection.disconnect();

    this.update(serviceName, statusCode, email, authenticationMethod, apiKey, zoneId, document);
    return new Pair<>(statusCode, document);
  }

  public Pair<Integer, JsonDocument> deleteRecord(String email,
    CloudflareConfigurationEntry.AuthenticationMethod authenticationMethod, String apiKey, String zoneId,
    String recordId) {
    Preconditions.checkNotNull(zoneId);
    Preconditions.checkNotNull(recordId);

    try {
      HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(
        CLOUDFLARE_API_V1 + "zones/" + zoneId + "/dns_records/" + recordId).openConnection();
      httpURLConnection.setRequestMethod("DELETE");

      this.initRequestProperties(httpURLConnection, email, authenticationMethod, apiKey);
      httpURLConnection.connect();

      int statusCode = httpURLConnection.getResponseCode();
      JsonDocument document = JsonDocument.newDocument(this.readResponse(httpURLConnection));
      httpURLConnection.disconnect();

      if (statusCode < 400 && document.getDocument("result") != null && document.getDocument("result").contains("id")) {
        this.createdRecords.remove(document.getDocument("result").getString("id"));
        this.write();
      } else {
        CloudNetDriver.getInstance().getLogger().fatal(document.toJson());
      }

      return new Pair<>(statusCode, document);

    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return null;
  }

  @Override
  public void close() {
  }

  private void read() {
    JsonDocument document = this.database.get(CLOUDFLARE_STORE_DOCUMENT);

    if (document == null) {
      document = new JsonDocument("cache", new HashMap<>());
    }

    this.createdRecords.clear();
    this.createdRecords.putAll(document.get("cache", MAP_STRING_DOCUMENT, new HashMap<>()));
  }

  private void write() {
    JsonDocument document = this.database.get(CLOUDFLARE_STORE_DOCUMENT);

    if (document == null) {
      document = new JsonDocument();
    }

    document.append("cache", this.createdRecords);

    this.database.update(CLOUDFLARE_STORE_DOCUMENT, document);
  }

  private void update(String serviceName, int statusCode, String email,
    CloudflareConfigurationEntry.AuthenticationMethod authenticationMethod, String apiKey, String zoneId,
    JsonDocument document) {
    if (statusCode < 400 && document.getDocument("result") != null && document.getDocument("result").contains("id")) {
      this.createdRecords.put(document.getDocument("result").getString("id"), new Pair<>(serviceName, document
        .append("email", email)
        .append("authenticationMethod", authenticationMethod)
        .append("apiKey", apiKey)
        .append("zoneId", zoneId)
      ));
      this.write();
    } else {
      CloudNetDriver.getInstance().getLogger().fatal(document.toJson());
    }
  }

  private void initRequestProperties(HttpURLConnection httpURLConnection, String email,
    CloudflareConfigurationEntry.AuthenticationMethod authenticationMethod, String apiKey) {
    Preconditions.checkNotNull(authenticationMethod);
    Preconditions.checkNotNull(apiKey);

    httpURLConnection.setUseCaches(false);

    if (authenticationMethod == CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY) {
      Preconditions.checkNotNull(email);

      httpURLConnection.setRequestProperty("X-Auth-Email", email);
      httpURLConnection.setRequestProperty("X-Auth-Key", apiKey);
    } else if (authenticationMethod == CloudflareConfigurationEntry.AuthenticationMethod.BEARER_TOKEN) {
      httpURLConnection.setRequestProperty("Authorization", "Bearer " + apiKey);
    }

    httpURLConnection.setRequestProperty("Accept", "application/json");
    httpURLConnection.setRequestProperty("Content-Type", "application/json");
  }

  private byte[] readResponse(HttpURLConnection httpURLConnection) {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

      try (InputStream inputStream = httpURLConnection.getInputStream()) {
        FileUtils.copy(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();

      } catch (Throwable throwable) {
        throwable.printStackTrace();
      }

      try (InputStream inputStream = httpURLConnection.getErrorStream()) {
        FileUtils.copy(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();

      } catch (Throwable throwable) {
        throwable.printStackTrace();
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }

    return null;
  }

  public Database getDatabase() {
    return this.database;
  }

  public Map<String, Pair<String, JsonDocument>> getCreatedRecords() {
    return this.createdRecords;
  }
}
