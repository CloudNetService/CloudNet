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

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.modules.dns.provider.DnsZoneProvider;
import eu.cloudnetservice.modules.dns.provider.info.DnsRecordInfo;
import eu.cloudnetservice.modules.dns.provider.record.AAAADnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.ADnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.DnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.SrvDnsRecordData;
import io.leangen.geantyref.TypeFactory;
import io.vavr.control.Try;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;

final class NetcupDnsZoneProvider implements DnsZoneProvider {

  private static final Type DOCUMENT_LIST_TYPE = TypeFactory.parameterizedClass(List.class, Document.class);

  private final String domainName;
  private final NetcupSoapRequestSender requestSender;

  public NetcupDnsZoneProvider(@NonNull String domainName, @NonNull NetcupSoapRequestSender requestSender) {
    this.domainName = domainName;
    this.requestSender = requestSender;
  }

  @Override
  public @NonNull Try<List<DnsRecordInfo>> listRecords() {
    var requestParams = Document.newJsonDocument().append("domainname", this.domainName);
    return this.requestSender.requestAuthenticated("infoDnsRecords", requestParams).map(responseData -> {
      List<Document> dnsRecords = responseData.readObject("dnsrecords", DOCUMENT_LIST_TYPE);
      return dnsRecords.stream()
        .map(dnsRecordData -> {
          var recordType = dnsRecordData.getString("type");
          var recordData = switch (recordType) {
            case "A" -> {
              var name = dnsRecordData.getString("hostname");
              var content = dnsRecordData.getString("destination");
              yield new ADnsRecordData(name, 0, content);
            }
            case "AAAA" -> {
              var name = dnsRecordData.getString("hostname");
              var content = dnsRecordData.getString("destination");
              yield new AAAADnsRecordData(name, 0, content);
            }
            case "SRV" -> {
              var name = dnsRecordData.getString("hostname");
              var contentParts = dnsRecordData.getString("destination").split(" ");
              var priority = Integer.parseInt(contentParts[0]);
              var weight = Integer.parseInt(contentParts[1]);
              var port = Integer.parseInt(contentParts[2]);
              var target = contentParts[3];
              yield new SrvDnsRecordData(name, 0, target, port, priority, weight);
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
    var serializedRecordData = this.serializeRecordData(recordInfo.data())
      .append("id", recordInfo.id())
      .append("deleterecord", true);
    var requestParams = this.constructUpdateRecordsParams(serializedRecordData);
    return this.requestSender.requestAuthenticated("updateDnsRecords", requestParams).map(_ -> null);
  }

  @Override
  public @NonNull Try<DnsRecordInfo> createDnsRecord(@NonNull DnsRecordData recordData) {
    var serializedRecordData = this.serializeRecordData(recordData);
    var requestParams = this.constructUpdateRecordsParams(serializedRecordData);
    return this.requestSender.requestAuthenticated("createDnsRecord", requestParams).map(responseData -> {
      List<Document> dnsRecords = responseData.readObject("dnsrecords", DOCUMENT_LIST_TYPE);
      return dnsRecords.stream()
        .filter(dnsRecordData -> requestParams.keys().stream().allMatch(key -> {
          var originalValue = requestParams.getString(key);
          var dnsRecordValue = dnsRecordData.getString(key);
          return originalValue.equals(dnsRecordValue);
        }))
        .findFirst()
        .map(dnsRecordData -> {
          var id = dnsRecordData.getString("id");
          return DnsRecordInfo.of(id, recordData);
        })
        .orElseThrow(() -> new IllegalStateException("Created record not found in response list"));
    });
  }

  @Override
  public @NonNull Try<DnsRecordInfo> updateDnsRecord(
    @NonNull DnsRecordInfo recordInfo,
    @NonNull DnsRecordData newRecordData
  ) {
    var serializedRecordData = this.serializeRecordData(newRecordData).append("id", recordInfo.id());
    var requestParams = this.constructUpdateRecordsParams(serializedRecordData);
    return this.requestSender.requestAuthenticated("updateDnsRecords", requestParams)
      .map(_ -> DnsRecordInfo.of(recordInfo.id(), newRecordData));
  }

  private @NonNull Document.Mutable serializeRecordData(@NonNull DnsRecordData recordData) {
    var recordValue = switch (recordData) {
      case ADnsRecordData(_, _, var content) -> content;
      case AAAADnsRecordData(_, _, var content) -> content;
      case SrvDnsRecordData(_, _, var target, var port, var priority, var weight) -> String.format(
        "%d %d %d %s",
        priority, weight, port, target);
    };

    return Document.newJsonDocument()
      .append("hostname", recordData.name())
      .append("type", recordData.type())
      .append("destination", recordValue);
  }

  private @NonNull Document constructUpdateRecordsParams(@NonNull Document recordData) {
    var recordSet = Document.newJsonDocument().append("dnsrecords", List.of(recordData));
    return Document.newJsonDocument()
      .append("domainname", this.domainName)
      .append("dnsrecordset", recordSet);
  }
}
