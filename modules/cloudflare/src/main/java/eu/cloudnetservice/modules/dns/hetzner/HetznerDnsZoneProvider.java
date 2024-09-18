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

package eu.cloudnetservice.modules.dns.hetzner;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.StandardSerialisationStyle;
import eu.cloudnetservice.modules.dns.provider.DnsZoneProvider;
import eu.cloudnetservice.modules.dns.provider.info.DnsRecordInfo;
import eu.cloudnetservice.modules.dns.provider.record.AAAADnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.ADnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.DnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.SrvDnsRecordData;
import eu.cloudnetservice.modules.dns.util.UnirestToDocumentTransformer;
import io.leangen.geantyref.TypeFactory;
import io.vavr.control.Try;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import kong.unirest.core.UnirestInstance;
import lombok.NonNull;

final class HetznerDnsZoneProvider implements DnsZoneProvider {

  private static final Type DOCUMENT_LIST_TYPE = TypeFactory.parameterizedClass(List.class, Document.class);

  private final String zoneId;
  private final UnirestInstance unirestInstance;

  public HetznerDnsZoneProvider(@NonNull String zoneId, @NonNull UnirestInstance unirestInstance) {
    this.zoneId = zoneId;
    this.unirestInstance = unirestInstance;
  }

  @Override
  public @NonNull Try<List<DnsRecordInfo>> listRecords() {
    return Try.of(() -> {
      var response = this.unirestInstance.get("/records")
        .queryString("zone_id", this.zoneId)
        .asObject(UnirestToDocumentTransformer.INSTANCE);
      if (response.isSuccess()) {
        List<Document> dnsRecords = response.getBody().readObject("records", DOCUMENT_LIST_TYPE);
        return dnsRecords.stream()
          .map(dnsRecordData -> {
            var recordType = dnsRecordData.getString("type");
            var recordData = switch (recordType) {
              case "A" -> {
                var name = dnsRecordData.getString("name");
                var ttl = dnsRecordData.getInt("ttl");
                var content = dnsRecordData.getString("value");
                yield new ADnsRecordData(name, ttl, content);
              }
              case "AAAA" -> {
                var name = dnsRecordData.getString("name");
                var ttl = dnsRecordData.getInt("ttl");
                var content = dnsRecordData.getString("value");
                yield new AAAADnsRecordData(name, ttl, content);
              }
              case "SRV" -> {
                var name = dnsRecordData.getString("name");
                var ttl = dnsRecordData.getInt("ttl");

                var contentParts = dnsRecordData.getString("value").split(" ");
                var priority = Integer.parseInt(contentParts[0]);
                var weight = Integer.parseInt(contentParts[1]);
                var port = Integer.parseInt(contentParts[2]);
                var target = contentParts[3];
                yield new SrvDnsRecordData(name, ttl, target, port, priority, weight);
              }
              default -> null;
            };

            if (recordData != null) {
              var recordId = dnsRecordData.getString("id");
              return DnsRecordInfo.of(recordId, recordData);
            } else {
              return null;
            }
          })
          .filter(Objects::nonNull)
          .toList();
      } else {
        var errorMessage = String.format(
          "Failed to list dns records of zone %s - server returned status %s (%s)",
          this.zoneId, response.getStatus(), response.getStatusText());
        throw new IllegalStateException(errorMessage);
      }
    });
  }

  @Override
  public @NonNull Try<Void> deleteDnsRecord(@NonNull DnsRecordInfo recordInfo) {
    return Try.of(() -> {
      var response = this.unirestInstance.delete("/records/{record_id}")
        .routeParam("record_id", recordInfo.id())
        .asEmpty();
      if (response.isSuccess()) {
        return null;
      } else {
        var errorMessage = String.format(
          "Failed to delete record %s from zone %s - server returned status %s (%s)",
          recordInfo.id(), this.zoneId, response.getStatus(), response.getStatusText());
        throw new IllegalStateException(errorMessage);
      }
    });
  }

  @Override
  public @NonNull Try<DnsRecordInfo> createDnsRecord(@NonNull DnsRecordData recordData) {
    var requestBodyContent = this.serializeRecordData(recordData);
    var requestBody = requestBodyContent.serializeToString(StandardSerialisationStyle.COMPACT);

    return Try.of(() -> {
      var response = this.unirestInstance.post("/records")
        .body(requestBody)
        .asObject(UnirestToDocumentTransformer.INSTANCE);
      if (response.isSuccess()) {
        var recordId = response.getBody().readDocument("record").getString("id");
        return DnsRecordInfo.of(recordId, recordData);
      } else {
        var errorMessage = String.format(
          "Unable to create record %s in zone %s - server returned status %s (%s)",
          recordData, this.zoneId, response.getStatus(), response.getStatusText());
        throw new IllegalStateException(errorMessage);
      }
    });
  }

  @Override
  public @NonNull Try<DnsRecordInfo> updateDnsRecord(
    @NonNull DnsRecordInfo recordInfo,
    @NonNull DnsRecordData newRecordData
  ) {
    var requestBodyContent = this.serializeRecordData(newRecordData);
    var requestBody = requestBodyContent.serializeToString(StandardSerialisationStyle.COMPACT);

    return Try.of(() -> {
      var response = this.unirestInstance.put("/records/{record_id}")
        .routeParam("record_id", recordInfo.id())
        .body(requestBody)
        .asObject(UnirestToDocumentTransformer.INSTANCE);
      if (response.isSuccess()) {
        return DnsRecordInfo.of(recordInfo.id(), newRecordData);
      } else {
        var errorMessage = String.format(
          "Unable to update record %s with %s in zone %s - server returned status %s (%s)",
          recordInfo.id(), newRecordData, this.zoneId, response.getStatus(), response.getStatusText());
        throw new IllegalStateException(errorMessage);
      }
    });
  }

  private @NonNull Document serializeRecordData(@NonNull DnsRecordData recordData) {
    var recordValue = switch (recordData) {
      case ADnsRecordData(_, _, var content) -> content;
      case AAAADnsRecordData(_, _, var content) -> content;
      case SrvDnsRecordData(_, _, var target, var port, var priority, var weight) -> String.format(
        "%d %d %d %s",
        priority, weight, port, target);
    };

    return Document.newJsonDocument()
      .append("zone_id", this.zoneId)
      .append("ttl", recordData.ttl())
      .append("type", recordData.type())
      .append("name", recordData.name())
      .append("value", recordValue);
  }
}
