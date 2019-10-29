package de.dytanic.cloudnet.ext.cloudflare.listener;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.service.CloudServicePostStartEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostStopEvent;
import de.dytanic.cloudnet.ext.cloudflare.CloudNetCloudflareModule;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareAPI;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareGroupConfiguration;
import de.dytanic.cloudnet.ext.cloudflare.dns.SRVRecord;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.function.BiConsumer;

public final class CloudflareStartAndStopListener {

    @EventListener
    public void handle(CloudServicePostStartEvent event) {
        this.handle0(event.getCloudService(), (cloudflareConfigurationEntry, cloudflareGroupConfiguration) -> {
            Pair<Integer, JsonDocument> response = CloudflareAPI.getInstance().createRecord(
                    event.getCloudService().getServiceId().getName(),
                    cloudflareConfigurationEntry.getEmail(),
                    cloudflareConfigurationEntry.getApiToken(),
                    cloudflareConfigurationEntry.getZoneId(),
                    new SRVRecord(
                            "_minecraft._tcp." + cloudflareConfigurationEntry.getDomainName(),
                            "SRV " + cloudflareGroupConfiguration.getPriority() + " " + cloudflareGroupConfiguration.getWeight() + " " +
                                    event.getCloudService().getServiceConfiguration().getPort() + " " +
                                    CloudNet.getInstance().getConfig().getIdentity().getUniqueId() + "." +
                                    cloudflareConfigurationEntry.getDomainName(),
                            "_minecraft",
                            "_tcp",
                            cloudflareGroupConfiguration.getSub().equals("@") ? cloudflareConfigurationEntry.getDomainName() : cloudflareGroupConfiguration.getSub(),
                            cloudflareGroupConfiguration.getPriority(),
                            cloudflareGroupConfiguration.getWeight(),
                            event.getCloudService().getServiceConfiguration().getPort(),
                            CloudNet.getInstance().getConfig().getIdentity().getUniqueId() + "." +
                                    cloudflareConfigurationEntry.getDomainName()
                    )
            );

            if (response.getFirst() < 400) {
                CloudNetDriver.getInstance().getLogger().info(LanguageManager.getMessage("module-cloudflare-create-dns-record-for-service")
                        .replace("%service%", event.getCloudService().getServiceId().getName())
                        .replace("%domain%", cloudflareConfigurationEntry.getDomainName())
                        .replace("%recordId%", response.getSecond().getDocument("result").getString("id"))
                );
            }
        });
    }

    @EventListener
    public void handle(CloudServicePostStopEvent event) {
        this.handle0(event.getCloudService(), (cloudflareConfigurationEntry, cloudflareGroupConfiguration) -> {
            Pair<String, JsonDocument> entry = Iterables.first(CloudflareAPI.getInstance().getCreatedRecords().values(), item -> item.getFirst().equalsIgnoreCase(event.getCloudService().getServiceId().getName()));

            if (entry != null) {
                Pair<Integer, JsonDocument> response = CloudflareAPI.getInstance().deleteRecord(
                        cloudflareConfigurationEntry.getEmail(),
                        cloudflareConfigurationEntry.getApiToken(),
                        cloudflareConfigurationEntry.getZoneId(),
                        entry.getSecond().getDocument("result").getString("id")
                );

                if (response.getFirst() < 400) {
                    CloudNetDriver.getInstance().getLogger().info(LanguageManager.getMessage("module-cloudflare-delete-dns-record-for-service")
                            .replace("%service%", event.getCloudService().getServiceId().getName())
                            .replace("%domain%", cloudflareConfigurationEntry.getDomainName())
                            .replace("%recordId%", response.getSecond().getDocument("result").getString("id"))
                    );
                }
            }
        });
    }


    private void handle0(ICloudService cloudService, BiConsumer<CloudflareConfigurationEntry, CloudflareGroupConfiguration> handler) {
        for (CloudflareConfigurationEntry entry : CloudNetCloudflareModule.getInstance().getCloudflareConfiguration().getEntries()) {
            if (entry != null && entry.isEnabled() && entry.getGroups() != null) {
                for (CloudflareGroupConfiguration groupConfiguration : entry.getGroups()) {
                    if (groupConfiguration != null && Iterables.contains(groupConfiguration.getName(), cloudService.getServiceConfiguration().getGroups())) {
                        handler.accept(entry, groupConfiguration);
                        break;
                    }
                }
            }
        }
    }
}