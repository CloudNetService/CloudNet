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

package eu.cloudnetservice.modules.dns.impl;

import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.dns.config.DnsModuleConfig;
import eu.cloudnetservice.modules.dns.config.DnsModuleConfigEntry;
import eu.cloudnetservice.modules.dns.config.DnsModuleGroupEntry;
import eu.cloudnetservice.modules.dns.config.DnsModuleGroupRecord;
import eu.cloudnetservice.modules.dns.config.DnsModuleProviderConfig;
import eu.cloudnetservice.modules.dns.impl._depreacted.CloudflareConfiguration;
import eu.cloudnetservice.modules.dns.impl._depreacted.CloudflareConfigurationEntry;
import eu.cloudnetservice.modules.dns.impl.listener.DnsServiceListener;
import eu.cloudnetservice.modules.dns.provider.DnsProvider;
import eu.cloudnetservice.modules.dns.provider.DnsZoneProvider;
import eu.cloudnetservice.modules.dns.provider.info.DnsRecordInfo;
import eu.cloudnetservice.modules.dns.provider.record.AAAADnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.ADnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.DnsRecordData;
import eu.cloudnetservice.modules.dns.provider.record.SrvDnsRecordData;
import eu.cloudnetservice.node.config.Configuration;
import eu.cloudnetservice.node.impl.util.NetworkUtil;
import eu.cloudnetservice.node.service.CloudService;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class CloudNetDnsModule extends DriverModule {

  private static final String LEGACY_MODULE_NAME = "CloudNet-Cloudflare";
  private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetDnsModule.class);

  private DnsModuleConfig configuration;

  @ModuleTask(lifecycle = ModuleLifeCycle.LOADED)
  public void convertConfiguration() {
    var documentFactory = DocumentFactory.json();
    if (this.readConfig(documentFactory).empty()) {
      // TODO was wenn der user gar keine alte config hat und das der erste start ist
      var legacyConfigPath = this.configPath()
        .resolve("../")
        .resolve(LEGACY_MODULE_NAME)
        .resolve("config.json");

      if (Files.notExists(legacyConfigPath)) {
        return;
      }

      var legacyConfig = documentFactory.parse(legacyConfigPath).toInstanceOf(CloudflareConfiguration.class);
      var targetConfig = new DnsModuleConfig(new ArrayList<>());
      for (var entry : legacyConfig.entries()) {
        if (entry.authenticationMethod() == CloudflareConfigurationEntry.AuthenticationMethod.GLOBAL_KEY) {
          LOGGER.warn(
            "Unable to convert configuration for entry {}: global key authentication is not supported",
            entry.entryName());
          continue;
        }

        var groupEntries = entry.groups().stream()
          .map(group -> DnsModuleGroupEntry.builder()
            .targetGroup(group.name())
            .records(List.of(DnsModuleGroupRecord.builder()
              .priority(group.priority())
              .weight(group.weight())
              .subdomain(group.sub())
              .build()))
            .build())
          .toList();

        var targetEntry = DnsModuleConfigEntry.builder()
          .groups(groupEntries)
          .enabled(entry.enabled())
          .domain(entry.domainName())
          .hostAddressV4(entry.hostAddress())
          .providerConfig(DnsModuleProviderConfig.builder()
            .name("cloudflare")
            .properties(documentFactory.newDocument()
              .append("apiKey", entry.apiToken())
              .append("zoneId", entry.zoneId()))
            .build())
          .build();

        targetConfig.entries().add(targetEntry);
      }

      this.writeConfig(documentFactory.newDocument(targetConfig));
    }
  }

  @ModuleTask(order = Byte.MAX_VALUE, lifecycle = ModuleLifeCycle.STARTED)
  public void discoverServices(@NonNull ServiceRegistry registry) {
    registry.discoverServices(CloudNetDnsModule.class);
  }

  @ModuleTask(order = 120, lifecycle = ModuleLifeCycle.STARTED)
  public void loadConfiguration() {
    this.configuration = this.readConfig(
      DnsModuleConfig.class,
      // TODO: proper default config
      () -> new DnsModuleConfig(List.of(
        DnsModuleConfigEntry.builder()
          .enabled(true)
          .hostAddressV4("127.0.0.1")
          .domain("cloudnetservice.eu")
          .groups(List.of(DnsModuleGroupEntry.builder()
            .targetGroup("Proxy")
            .build()))
          .providerConfig(DnsModuleProviderConfig.builder()
            .name("cloudflare")
            .properties(DocumentFactory.json().newDocument()
              .append("apiKey", "")
              .append("zoneId", ""))
            .build())
          .build()
      )),
      DocumentFactory.json());
  }

  /*

  "domain": "playo.dev",
      "domainNamespace": null,
      "hostAddressV4": "127.0.0.1",
      "hostAddressV6": "0:0:0:0:0:0:0:1",
      "groups": [
        {
          "targetGroup": "Proxy",
          "hostAddressV4": null,
          "hostAddressV6": null,
          "records": [
            {
              "subdomain": "@"
             },
             {
              "subdomain": "velocity",
              }
          ]
        }
      ],

      -> A node-1.playo.dev -> 127.0.0.1
      -> AAAA node-1.playo.dev -> 0:0:0:0:0:0:0:1

      Für jeden Proxy:

      -> SRV playo.dev -> PORT node-1.playo.dev
      -> SRV velocity.playo.dev -> PORT node-1.playo.dev



      "domain": "playo.dev",
      "domainNamespace": null,
      "hostAddressV4": "127.0.0.1",
      "hostAddressV6": "0:0:0:0:0:0:0:1",
      "groups": [
        {
          "targetGroup": "Proxy",
          "hostAddressV4": "10.10.10.10",
          "hostAddressV6": null,
          "records": [
            {
              "subdomain": "@"
             },
             {
              "subdomain": "velocity",
              }
          ]
        }
      ],

      -> A node-1.playo.dev -> 127.0.0.1
      -> AAAA node-1.playo.dev -> 0:0:0:0:0:0:0:1
      -> A md5(node-1-Proxy-10.10.10.10).playo.dev -> 10.10.10.10
      -> KEIN AAAA Record da null

      Für jeden Proxy:

      -> SRV playo.dev -> PORT md5(node-1-Proxy-10.10.10.10).playo.dev
      -> SRV velocity.playo.dev -> PORT md5(node-1-Proxy-10.10.10.10).playo.dev

      "domain": "playo.dev",
      "domainNamespace": null,
      "hostAddressV4": "127.0.0.1",
      "hostAddressV6": "0:0:0:0:0:0:0:1",
      "groups": [
        {
          "targetGroup": "Proxy",
          "hostAddressV4": "10.10.10.10",
          "hostAddressV6": "fd00::1",
          "records": [
            {
              "subdomain": "@"
             },
             {
              "subdomain": "velocity",
              }
          ]
        }
      ],

      -> A node-1.playo.dev -> 127.0.0.1
      -> AAAA node-1.playo.dev -> 0:0:0:0:0:0:0:1
      -> A md5(node-1-Proxy-10.10.10.10).playo.dev -> 10.10.10.10
      -> AAAA md5(node-1-Proxy-fd00::1).playo.dev -> fd00::1

      Für jeden Proxy:
      -> SRV playo.dev -> PORT md5(node-1-Proxy-10.10.10.10).playo.dev
      -> SRV playo.dev -> PORT md5(node-1-Proxy-fd00::1).playo.dev
      -> SRV velocity.playo.dev -> PORT md5(node-1-Proxy-10.10.10.10).playo.dev
      -> SRV velocity.playo.dev -> PORT md5(node-1-Proxy-fd00::1).playo.dev

   */

  @ModuleTask(order = 110, lifecycle = ModuleLifeCycle.STARTED)
  public void registerDnsProvider(@NonNull ServiceRegistry registry, @NonNull Configuration configuration) {
    for (var entry : this.configuration.entries()) {
      if (entry.enabled()) {
        var zoneProvider = this.zoneProvider(registry, entry);
        if (zoneProvider != null) {
          this.syncAddressRecords(zoneProvider, entry, configuration);
        }
      }
    }
  }

  @ModuleTask(order = 105, lifecycle = ModuleLifeCycle.STARTED)
  public void registerListeners(@NonNull EventManager eventManager, @NonNull DnsServiceListener listener) {
    eventManager.registerListener(listener);
  }

  @ModuleTask(order = 100, lifecycle = ModuleLifeCycle.STARTED)
  public void syncRunningServices(
    @NonNull DnsServiceListener listener,
    @NonNull CloudServiceManager serviceManager,
    @NonNull Configuration configuration,
    @NonNull ServiceRegistry registry
  ) {
    for (var service : serviceManager.localCloudServices()) {
      listener.syncService(service, configuration, registry);
    }
  }

  public @NonNull DnsModuleConfig configuration() {
    return this.configuration;
  }

  public @Nullable DnsZoneProvider zoneProvider(
    @NonNull ServiceRegistry registry,
    @NonNull DnsModuleConfigEntry entry
  ) {
    var providerConfig = entry.providerConfig();
    var provider = registry.instance(DnsProvider.class, providerConfig.name());
    if (provider == null) {
      LOGGER.warn("Unable to sync DNS records for zone {}: provider {} is not registered", entry.domain(),
        providerConfig.name());
      return null;
    }

    return provider.zoneProvider(providerConfig.toProviderZoneConfig());
  }

  public void syncAddressRecords(
    @NonNull DnsZoneProvider zoneProvider,
    @NonNull DnsModuleConfigEntry entry,
    @NonNull Configuration configuration
  ) {
    zoneProvider.listRecords()
      .onSuccess(records -> {
        var nodeRecordName = this.nodeRecordName(entry, configuration);
        var hostAddressV4 = this.resolveHostAddress(entry.hostAddressV4(), configuration);
        if (hostAddressV4 != null) {
          this.syncAddressRecord(zoneProvider, records, new ADnsRecordData(nodeRecordName, 0, hostAddressV4));
        }

        var hostAddressV6 = this.resolveHostAddress(entry.hostAddressV6(), configuration);
        if (hostAddressV6 != null) {
          this.syncAddressRecord(zoneProvider, records, new AAAADnsRecordData(nodeRecordName, 0, hostAddressV6));
        }

        for (var group : entry.groups()) {
          for (var target : this.groupSpecificTargets(entry, group, configuration)) {
            this.syncAddressRecord(zoneProvider, records, target.toAddressRecord(0));
          }
        }
      })
      .onFailure(throwable -> LOGGER.warn(
        "Could not read records for zone {}: {}",
        entry.domain(),
        throwable.getMessage()));
  }

  public @NonNull List<DnsTarget> serviceTargets(
    @NonNull DnsModuleConfigEntry entry,
    @NonNull DnsModuleGroupEntry group,
    @NonNull CloudService service,
    @NonNull Configuration configuration
  ) {
    var groupTargets = this.groupSpecificTargets(entry, group, configuration);
    if (!groupTargets.isEmpty() || this.hasCustomGroupTarget(group)) {
      return groupTargets;
    }

    var nodeTargets = new ArrayList<DnsTarget>();
    var nodeRecordName = this.nodeRecordName(entry, configuration);
    var entryHostV4 = this.resolveHostAddress(entry.hostAddressV4(), configuration);
    var entryHostV6 = this.resolveHostAddress(entry.hostAddressV6(), configuration);
    if (entryHostV4 != null) {
      nodeTargets.add(new DnsTarget(nodeRecordName, "A", entryHostV4));
    }
    if (entryHostV6 != null) {
      nodeTargets.add(new DnsTarget(nodeRecordName, "AAAA", entryHostV6));
    }

    if (!nodeTargets.isEmpty()) {
      return nodeTargets;
    }

    var serviceHost = this.resolveHostAddress(service.serviceConfiguration().hostAddress(), configuration);
    if (serviceHost == null) {
      serviceHost = this.resolveHostAddress(configuration.hostAddress(), configuration);
    }

    if (serviceHost == null) {
      return List.of();
    }

    var targetType = serviceHost.indexOf(':') == -1 ? "A" : "AAAA";
    var targetName = this.hashedRecordName(entry, configuration, group.targetGroup(), serviceHost);
    return List.of(new DnsTarget(targetName, targetType, serviceHost));
  }

  public void syncServiceRecords(
    @NonNull DnsZoneProvider zoneProvider,
    @NonNull DnsModuleConfigEntry entry,
    @NonNull DnsModuleGroupEntry group,
    @NonNull CloudService service,
    @NonNull Configuration configuration
  ) {
    zoneProvider.listRecords()
      .onSuccess(records -> {
        var targets = this.serviceTargets(entry, group, service, configuration);
        if (targets.isEmpty()) {
          LOGGER.warn(
            "Unable to create DNS records for service {} in group {}: no valid target host address",
            service.serviceId().name(),
            group.targetGroup());
          return;
        }

        for (var target : targets) {
          this.syncAddressRecord(zoneProvider, records, target.toAddressRecord(0));
        }

        for (var record : group.records()) {
          for (var target : targets) {
            var srvRecord = new SrvDnsRecordData(
              this.serviceRecordName(entry, record),
              record.ttl(),
              target.name(),
              service.serviceConfiguration().port(),
              record.priority(),
              record.weight());
            this.syncSrvRecord(zoneProvider, records, srvRecord);
          }
        }
      })
      .onFailure(throwable -> LOGGER.warn(
        "Could not sync DNS records for service {}: {}",
        service.serviceId().name(),
        throwable.getMessage()));
  }

  public void deleteServiceRecords(
    @NonNull DnsZoneProvider zoneProvider,
    @NonNull DnsModuleConfigEntry entry,
    @NonNull DnsModuleGroupEntry group,
    @NonNull CloudService service,
    @NonNull Configuration configuration
  ) {
    zoneProvider.listRecords()
      .onSuccess(records -> {
        var targets = this.serviceTargets(entry, group, service, configuration);
        for (var record : group.records()) {
          for (var target : targets) {
            var expectedRecord = new SrvDnsRecordData(
              this.serviceRecordName(entry, record),
              record.ttl(),
              target.name(),
              service.serviceConfiguration().port(),
              record.priority(),
              record.weight());
            records.stream()
              .filter(recordInfo -> this.isSameSrvEndpoint(recordInfo.data(), expectedRecord))
              .forEach(recordInfo -> this.deleteDnsRecord(zoneProvider, recordInfo));
          }
        }
      })
      .onFailure(throwable -> LOGGER.warn(
        "Could not delete DNS records for service {}: {}",
        service.serviceId().name(),
        throwable.getMessage()));
  }

  // TODO consider if this should be public and maybe also might go into a different class at all
  public @Nullable String resolveHostAddress(@Nullable String hostAddress, @NonNull Configuration config) {
    if (hostAddress == null || hostAddress.isEmpty()) {
      return null;
    }

    var parsedHost = NetworkUtil.parseHostAndPort(hostAddress, false);
    if (parsedHost != null) {
      return parsedHost.host();
    }

    var alias = config.ipAliases().get(hostAddress);
    if (alias != null) {
      var parsedAlias = NetworkUtil.parseHostAndPort(alias, false);
      if (parsedAlias != null) {
        return parsedAlias.host();
      }
    }

    return null;
  }

  private @NonNull List<DnsTarget> groupSpecificTargets(
    @NonNull DnsModuleConfigEntry entry,
    @NonNull DnsModuleGroupEntry group,
    @NonNull Configuration configuration
  ) {
    var targets = new ArrayList<DnsTarget>();
    var hostAddressV4 = this.resolveHostAddress(group.hostAddressV4(), configuration);
    if (hostAddressV4 != null) {
      targets.add(new DnsTarget(
        this.hashedRecordName(entry, configuration, group.targetGroup(), hostAddressV4),
        "A",
        hostAddressV4));
    }

    var hostAddressV6 = this.resolveHostAddress(group.hostAddressV6(), configuration);
    if (hostAddressV6 != null) {
      targets.add(new DnsTarget(
        this.hashedRecordName(entry, configuration, group.targetGroup(), hostAddressV6),
        "AAAA",
        hostAddressV6));
    }

    return targets;
  }

  private boolean hasCustomGroupTarget(@NonNull DnsModuleGroupEntry group) {
    return this.hasConfiguredHostAddress(group.hostAddressV4()) || this.hasConfiguredHostAddress(group.hostAddressV6());
  }

  private boolean hasConfiguredHostAddress(@Nullable String hostAddress) {
    return hostAddress != null && !hostAddress.isEmpty();
  }

  private @NonNull String nodeRecordName(
    @NonNull DnsModuleConfigEntry entry,
    @NonNull Configuration configuration
  ) {
    return this.fullRecordName(configuration.identity().uniqueId(), entry);
  }

  private @NonNull String hashedRecordName(
    @NonNull DnsModuleConfigEntry entry,
    @NonNull Configuration configuration,
    @NonNull String group,
    @NonNull String hostAddress
  ) {
    var hashSource = "%s-%s-%s".formatted(configuration.identity().uniqueId(), group, hostAddress);
    return this.fullRecordName(this.md5Hex(hashSource), entry);
  }

  private @NonNull String serviceRecordName(
    @NonNull DnsModuleConfigEntry entry,
    @NonNull DnsModuleGroupRecord record
  ) {
    return this.fullRecordName(record.subdomain(), entry.domain());
  }

  private @NonNull String fullRecordName(@NonNull String name, @NonNull DnsModuleConfigEntry entry) {
    var namespace = entry.domainNamespace();
    if (namespace == null || namespace.isEmpty()) {
      return this.fullRecordName(name, entry.domain());
    } else {
      return this.fullRecordName("%s.%s".formatted(name, namespace), entry.domain());
    }
  }

  private @NonNull String fullRecordName(@NonNull String name, @NonNull String domain) {
    if (name.isEmpty() || name.equals("@")) {
      return domain;
    } else if (name.endsWith("." + domain) || name.equals(domain)) {
      return name;
    } else {
      return "%s.%s".formatted(name, domain);
    }
  }

  private @NonNull String md5Hex(@NonNull String input) {
    try {
      var digest = MessageDigest.getInstance("MD5");
      var hashedInput = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      var builder = new StringBuilder(hashedInput.length * 2);
      for (var hashedByte : hashedInput) {
        builder.append(Character.forDigit((hashedByte >> 4) & 0xF, 16));
        builder.append(Character.forDigit(hashedByte & 0xF, 16));
      }

      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("MD5 digest is not available", exception);
    }
  }

  private void syncAddressRecord(
    @NonNull DnsZoneProvider zoneProvider,
    @NonNull List<DnsRecordInfo> records,
    @NonNull DnsRecordData desiredRecord
  ) {
    var existingRecord = records.stream()
      .filter(record -> this.isSameRecord(record.data(), desiredRecord))
      .findFirst()
      .orElse(null);

    if (existingRecord == null) {
      LOGGER.debug("Creating new DNS record {}", desiredRecord);
      zoneProvider.createDnsRecord(desiredRecord).onFailure(throwable -> LOGGER.warn(
        "Could not create DNS record {}: {}",
        desiredRecord,
        throwable.getMessage()));
    } else {
      if (this.needsUpdate(existingRecord.data(), desiredRecord)) {
        LOGGER.debug("Updating DNS record {} with {}", existingRecord.id(), desiredRecord);
        zoneProvider.updateDnsRecord(existingRecord, desiredRecord).onFailure(throwable -> LOGGER.warn(
          "Could not update DNS record {} with {}: {}",
          existingRecord.id(),
          desiredRecord,
          throwable.getMessage()));
      } else {
        LOGGER.debug("DNS record {} already exists", desiredRecord);
      }
    }
  }

  private void syncSrvRecord(
    @NonNull DnsZoneProvider zoneProvider,
    @NonNull List<DnsRecordInfo> records,
    @NonNull SrvDnsRecordData desiredRecord
  ) {
    var existingRecord = records.stream()
      .filter(record -> this.isSameSrvEndpoint(record.data(), desiredRecord))
      .findFirst()
      .orElse(null);

    if (existingRecord == null) {
      LOGGER.debug("Creating new DNS record {}", desiredRecord);
      zoneProvider.createDnsRecord(desiredRecord).onFailure(throwable -> LOGGER.warn(
        "Could not create DNS record {}: {}",
        desiredRecord,
        throwable.getMessage()));
    } else if (this.needsUpdate(existingRecord.data(), desiredRecord)) {
      LOGGER.debug("Updating DNS record {} with {}", existingRecord.id(), desiredRecord);
      zoneProvider.updateDnsRecord(existingRecord, desiredRecord).onFailure(throwable -> LOGGER.warn(
        "Could not update DNS record {} with {}: {}",
        existingRecord.id(),
        desiredRecord,
        throwable.getMessage()));
    } else {
      LOGGER.debug("DNS record {} already exists", desiredRecord);
    }
  }

  private void deleteDnsRecord(@NonNull DnsZoneProvider zoneProvider, @NonNull DnsRecordInfo recordInfo) {
    LOGGER.debug("Deleting DNS record {}", recordInfo.data());
    zoneProvider.deleteDnsRecord(recordInfo).onFailure(throwable -> LOGGER.warn(
      "Could not delete DNS record {}: {}",
      recordInfo.data(),
      throwable.getMessage()));
  }

  private boolean isSameRecord(@NonNull DnsRecordData currentRecord, @NonNull DnsRecordData desiredRecord) {
    return currentRecord.name().equalsIgnoreCase(desiredRecord.name())
      && currentRecord.type().equalsIgnoreCase(desiredRecord.type());
  }

  private boolean isSameSrvEndpoint(@NonNull DnsRecordData currentRecord, @NonNull SrvDnsRecordData desiredRecord) {
    if (currentRecord instanceof SrvDnsRecordData srvRecord) {
      return srvRecord.name().equalsIgnoreCase(desiredRecord.name())
        && srvRecord.target().equalsIgnoreCase(desiredRecord.target())
        && srvRecord.port() == desiredRecord.port();
    }

    return false;
  }

  private boolean needsUpdate(@NonNull DnsRecordData currentRecord, @NonNull DnsRecordData desiredRecord) {
    if (!currentRecord.content().equalsIgnoreCase(desiredRecord.content())) {
      return true;
    }

    if (currentRecord.ttl() > 0 && currentRecord.ttl() != desiredRecord.ttl()) {
      return true;
    }

    if (currentRecord instanceof SrvDnsRecordData currentSrvRecord
      && desiredRecord instanceof SrvDnsRecordData desiredSrvRecord) {
      return currentSrvRecord.port() != desiredSrvRecord.port()
        || currentSrvRecord.priority() != desiredSrvRecord.priority()
        || currentSrvRecord.weight() != desiredSrvRecord.weight();
    }

    return false;
  }

  public record DnsTarget(@NonNull String name, @NonNull String type, @NonNull String hostAddress) {

    private @NonNull DnsRecordData toAddressRecord(int ttl) {
      return switch (this.type) {
        case "A" -> new ADnsRecordData(this.name, ttl, this.hostAddress);
        case "AAAA" -> new AAAADnsRecordData(this.name, ttl, this.hostAddress);
        default -> throw new IllegalArgumentException("Unsupported address record type " + this.type);
      };
    }
  }
}
