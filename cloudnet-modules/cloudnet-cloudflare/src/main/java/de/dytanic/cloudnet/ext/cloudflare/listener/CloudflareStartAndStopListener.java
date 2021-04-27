package de.dytanic.cloudnet.ext.cloudflare.listener;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.event.service.CloudServicePostStartEvent;
import de.dytanic.cloudnet.event.service.CloudServicePostStopEvent;
import de.dytanic.cloudnet.ext.cloudflare.CloudNetCloudflareModule;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareConfigurationEntry;
import de.dytanic.cloudnet.ext.cloudflare.CloudflareGroupConfiguration;
import de.dytanic.cloudnet.ext.cloudflare.cloudflare.CloudFlareAPI;
import de.dytanic.cloudnet.ext.cloudflare.cloudflare.DnsRecordDetail;
import de.dytanic.cloudnet.ext.cloudflare.dns.SRVRecord;
import de.dytanic.cloudnet.service.ICloudService;

import java.util.Arrays;
import java.util.function.BiConsumer;

public final class CloudflareStartAndStopListener {

    private final CloudFlareAPI cloudFlareAPI;

    public CloudflareStartAndStopListener(CloudFlareAPI cloudFlareAPI) {
        this.cloudFlareAPI = cloudFlareAPI;
    }

    @EventListener
    public void handle(CloudServicePostStartEvent event) {
        this.handle0(event.getCloudService(), (entry, configuration) -> {
            DnsRecordDetail recordDetail = this.cloudFlareAPI.createRecord(
                    event.getCloudService().getServiceId().getUniqueId(),
                    entry,
                    SRVRecord.forConfiguration(entry, configuration, event.getCloudService().getServiceConfiguration().getPort())
            );

            if (recordDetail != null) {
                CloudNetDriver.getInstance().getLogger().info(LanguageManager.getMessage("module-cloudflare-create-dns-record-for-service")
                        .replace("%service%", event.getCloudService().getServiceId().getName())
                        .replace("%domain%", entry.getDomainName())
                        .replace("%recordId%", recordDetail.getId())
                );
            }
        });
    }

    @EventListener
    public void handle(CloudServicePostStopEvent event) {
        this.handle0(event.getCloudService(), (entry, configuration) -> {
            for (DnsRecordDetail detail : this.cloudFlareAPI.deleteAllRecords(event.getCloudService())) {
                CloudNetDriver.getInstance().getLogger().info(LanguageManager.getMessage("module-cloudflare-delete-dns-record-for-service")
                        .replace("%service%", event.getCloudService().getServiceId().getName())
                        .replace("%domain%", entry.getDomainName())
                        .replace("%recordId%", detail.getId())
                );
            }
        });
    }

    private void handle0(ICloudService cloudService, BiConsumer<CloudflareConfigurationEntry, CloudflareGroupConfiguration> handler) {
        for (CloudflareConfigurationEntry entry : CloudNetCloudflareModule.getInstance().getCloudflareConfiguration().getEntries()) {
            if (entry != null && entry.isEnabled() && entry.getGroups() != null && !entry.getGroups().isEmpty()) {
                for (CloudflareGroupConfiguration groupConfiguration : entry.getGroups()) {
                    if (groupConfiguration != null && Arrays.binarySearch(cloudService.getServiceConfiguration().getGroups(), groupConfiguration.getName()) >= 0) {
                        handler.accept(entry, groupConfiguration);
                    }
                }
            }
        }
    }
}