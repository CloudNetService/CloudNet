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

package eu.cloudnetservice.modules.dns.cloudflare;

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
import kong.unirest.core.HttpResponse;
import kong.unirest.core.PagedList;
import kong.unirest.core.UnirestInstance;
import lombok.NonNull;

final class CloudflareDnsZoneProvider implements DnsZoneProvider {

  private static final Type DOCUMENT_LIST_TYPE = TypeFactory.parameterizedClass(List.class, Document.class);

  private final String zoneId;
  private final UnirestInstance unirestInstance;

  public CloudflareDnsZoneProvider(@NonNull String zoneId, @NonNull UnirestInstance unirestInstance) {
    this.zoneId = zoneId;
    this.unirestInstance = unirestInstance;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NonNull Try<List<DnsRecordInfo>> listRecords() {
    return Try.of(() -> {
      PagedList<Document> allResponses = this.unirestInstance.get("/zones/{zone_id}/dns_records")
        .routeParam("zone_id", this.zoneId)
        .asPaged(
          request -> request.asObject(UnirestToDocumentTransformer.INSTANCE),
          nextResponse -> {
            if (nextResponse.isSuccess()) {
              var resultInfo = nextResponse.getBody().readDocument("result_info");
              var currentPage = resultInfo.getInt("page");
              var totalPages = resultInfo.getInt("total_pages");
              if (totalPages > currentPage) {
                return String.format("/zones/%s/dns_records?page=%d", this.zoneId, currentPage + 1);
              } else {
                return null;
              }
            } else {
              var errorMessage = String.format(
                "Failed to list dns records of zone %s - server returned status %s (%s)",
                this.zoneId, nextResponse.getStatus(), nextResponse.getStatusText());
              throw new IllegalStateException(errorMessage);
            }
          });
      return allResponses.stream()
        .map(HttpResponse::getBody)
        .flatMap(responseBody -> {
          List<Document> dnsRecords = responseBody.readObject("result", DOCUMENT_LIST_TYPE);
          return dnsRecords.stream();
        })
        .map(dnsRecordData -> {
          var recordType = dnsRecordData.getString("type");
          var recordData = switch (recordType) {
            case "A" -> {
              var name = dnsRecordData.getString("name");
              var ttl = dnsRecordData.getInt("ttl");
              var content = dnsRecordData.getString("content");
              yield new ADnsRecordData(name, ttl, content);
            }
            case "AAAA" -> {
              var name = dnsRecordData.getString("name");
              var ttl = dnsRecordData.getInt("ttl");
              var content = dnsRecordData.getString("content");
              yield new AAAADnsRecordData(name, ttl, content);
            }
            case "SRV" -> {
              var name = dnsRecordData.getString("name");
              var ttl = dnsRecordData.getInt("ttl");

              var data = dnsRecordData.readDocument("data");
              var port = data.getInt("port");
              var priority = data.getInt("priority");
              var target = data.getString("target");
              var weight = data.getInt("weight");
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
    });
  }

  @Override
  public @NonNull Try<Void> deleteDnsRecord(@NonNull DnsRecordInfo recordInfo) {
    return Try.of(() -> {
      var response = this.unirestInstance.delete("/zones/{zone_id}/dns_records/{dns_record_id}")
        .routeParam("zone_id", this.zoneId)
        .routeParam("dns_record_id", recordInfo.id())
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
      var response = this.unirestInstance.post("/zones/{zone_id}/dns_records")
        .routeParam("zone_id", this.zoneId)
        .body(requestBody)
        .asObject(UnirestToDocumentTransformer.INSTANCE);
      if (response.isSuccess()) {
        var recordId = response.getBody().readDocument("result").getString("id");
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
      var response = this.unirestInstance.put("/zones/{zone_id}/dns_records/{dns_record_id}")
        .routeParam("zone_id", this.zoneId)
        .routeParam("dns_record_id", recordInfo.id())
        .body(requestBody)
        .asEmpty();
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
    return switch (recordData) {
      case ADnsRecordData(var name, var ttl, var content) -> Document
        .newJsonDocument()
        .append("name", name)
        .append("ttl", ttl)
        .append("content", content)
        .append("type", "A");
      case AAAADnsRecordData(var name, var ttl, var content) -> Document
        .newJsonDocument()
        .append("name", name)
        .append("ttl", ttl)
        .append("content", content)
        .append("type", "AAAA");
      case SrvDnsRecordData(var name, var ttl, var target, var port, var priority, var weight) -> Document
        .newJsonDocument()
        .append("name", name)
        .append("ttl", ttl)
        .append("type", "SRV")
        .append("data", Document.newJsonDocument()
          .append("port", port)
          .append("priority", priority)
          .append("target", target)
          .append("weight", weight));
    };
  }
}
