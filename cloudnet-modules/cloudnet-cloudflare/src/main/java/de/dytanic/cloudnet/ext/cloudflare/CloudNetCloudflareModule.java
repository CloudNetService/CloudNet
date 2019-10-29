package de.dytanic.cloudnet.ext.cloudflare;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.ext.cloudflare.dns.DNSType;
import de.dytanic.cloudnet.ext.cloudflare.dns.DefaultDNSRecord;
import de.dytanic.cloudnet.ext.cloudflare.http.V1CloudflareConfigurationHttpHandler;
import de.dytanic.cloudnet.ext.cloudflare.listener.CloudflareStartAndStopListener;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

import java.net.InetAddress;
import java.util.Map;

public final class CloudNetCloudflareModule extends NodeCloudNetModule {

    private static CloudNetCloudflareModule instance;

    private CloudflareConfiguration cloudflareConfiguration;

    public CloudNetCloudflareModule() {
        instance = this;
    }

    public static CloudNetCloudflareModule getInstance() {
        return CloudNetCloudflareModule.instance;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
    public void loadConfiguration() {
        this.cloudflareConfiguration = getConfig().get("config", CloudflareConfiguration.TYPE, new CloudflareConfiguration(
                Iterables.newArrayList(new CloudflareConfigurationEntry[]{
                        new CloudflareConfigurationEntry(
                                false,
                                getInitialHostAddress(),
                                "user@example.com",
                                "api_token_string",
                                "zoneId",
                                "example.com",
                                Iterables.newArrayList(
                                        new CloudflareGroupConfiguration[]{
                                                new CloudflareGroupConfiguration("Proxy", "@", 1, 1)
                                        }
                                )
                        )
                })
        ));

        saveConfig();
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
    public void initCloudflareAPI() {
        new CloudflareAPI(getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME));
    }

    @ModuleTask(order = 125, event = ModuleLifeCycle.STARTED)
    public void addedDefaultCloudflareDNSServices() {
        for (CloudflareConfigurationEntry cloudflareConfigurationEntry : this.getCloudflareConfiguration().getEntries()) {
            if (cloudflareConfigurationEntry.isEnabled()) {
                Pair<Integer, JsonDocument> response = CloudflareAPI.getInstance().createRecord(
                        getCloudNet().getConfig().getIdentity().getUniqueId(),
                        cloudflareConfigurationEntry.getEmail(),
                        cloudflareConfigurationEntry.getApiToken(),
                        cloudflareConfigurationEntry.getZoneId(),
                        new DefaultDNSRecord(
                                DNSType.A,
                                getCloudNetConfig().getIdentity().getUniqueId() + "."
                                        + cloudflareConfigurationEntry.getDomainName(),
                                cloudflareConfigurationEntry.getHostAddress(),
                                new JsonDocument().toJsonObject()
                        )
                );

                if (response.getFirst() < 400) {
                    CloudNetDriver.getInstance().getLogger().info(LanguageManager.getMessage("module-cloudflare-create-dns-record-for-service")
                            .replace("%service%", getCloudNet().getConfig().getIdentity().getUniqueId())
                            .replace("%domain%", cloudflareConfigurationEntry.getDomainName())
                            .replace("%recordId%", response.getSecond().getDocument("result").getString("id"))
                    );
                }
            }
        }
    }

    @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
    public void registerListeners() {
        registerListener(new CloudflareStartAndStopListener());
    }

    @ModuleTask(order = 123, event = ModuleLifeCycle.STARTED)
    public void registerHttpHandlers() {
        getHttpServer().registerHandler("/api/v1/modules/cloudflare/config",
                new V1CloudflareConfigurationHttpHandler("cloudnet.http.v1.modules.cloudflare.config"));
    }

    public void updateConfiguration(CloudflareConfiguration cloudflareConfiguration) {
        this.cloudflareConfiguration = cloudflareConfiguration;

        getConfig().append("config", cloudflareConfiguration);
        saveConfig();
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STOPPED)
    public void closeCloudflareAPI() {
        if (CloudflareAPI.getInstance() != null) {
            try {
                CloudflareAPI.getInstance().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @ModuleTask(order = 64, event = ModuleLifeCycle.STOPPED)
    public void removeRecordsOnDelete() {
        for (Map.Entry<String, Pair<String, JsonDocument>> entry : CloudflareAPI.getInstance().getCreatedRecords().entrySet()) {
            CloudflareAPI.getInstance().deleteRecord(
                    entry.getValue().getSecond().getString("email"),
                    entry.getValue().getSecond().getString("apiKey"),
                    entry.getValue().getSecond().getString("zoneId"),
                    entry.getKey()
            );

            try {
                Thread.sleep(400);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }


    private String getInitialHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ex) {
            return "0.0.0.0";
        }
    }

    public CloudflareConfiguration getCloudflareConfiguration() {
        return this.cloudflareConfiguration;
    }

    public void setCloudflareConfiguration(CloudflareConfiguration cloudflareConfiguration) {
        this.cloudflareConfiguration = cloudflareConfiguration;
    }
}