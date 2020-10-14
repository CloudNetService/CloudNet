package de.dytanic.cloudnet.ext.cloudflare;

import com.google.gson.JsonObject;
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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
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
        this.cloudflareConfiguration = this.getConfig().get("config", CloudflareConfiguration.TYPE, new CloudflareConfiguration(
                new ArrayList<>(Collections.singletonList(
                        new CloudflareConfigurationEntry(
                                false,
                                this.getInitialHostAddress(),
                                "user@example.com",
                                "api_token_string",
                                "zoneId",
                                "example.com",
                                new ArrayList<>(Collections.singletonList(
                                        new CloudflareGroupConfiguration("Proxy", "@", 1, 1)
                                ))
                        )
                ))
        ));

        this.updateConfiguration(this.cloudflareConfiguration);
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.STARTED)
    public void initCloudflareAPI() {
        if (this.cloudflareConfiguration.getEntries().stream().noneMatch(CloudflareConfigurationEntry::isEnabled)) {
            return;
        }

        new CloudflareAPI(this.getDatabaseProvider().getDatabase(DefaultModuleHelper.DEFAULT_CONFIGURATION_DATABASE_NAME));
    }

    @ModuleTask(order = 125, event = ModuleLifeCycle.STARTED)
    public void addedDefaultCloudflareDNSServices() {
        if (CloudflareAPI.getInstance() == null) {
            return;
        }

        for (CloudflareConfigurationEntry cloudflareConfigurationEntry : this.getCloudflareConfiguration().getEntries()) {
            if (cloudflareConfigurationEntry.isEnabled()) {
                String hostAddress = cloudflareConfigurationEntry.getHostAddress();

                boolean ipv6Address;

                try {
                    ipv6Address = InetAddress.getByName(hostAddress) instanceof Inet6Address;
                } catch (UnknownHostException exception) {
                    this.getLogger().error("hostAddress of entry is invalid!", exception);
                    continue;
                }

                Pair<Integer, JsonDocument> response = CloudflareAPI.getInstance().createRecord(
                        this.getCloudNet().getConfig().getIdentity().getUniqueId(),
                        cloudflareConfigurationEntry.getEmail(),
                        cloudflareConfigurationEntry.getAuthenticationMethod(),
                        cloudflareConfigurationEntry.getApiToken(),
                        cloudflareConfigurationEntry.getZoneId(),
                        new DefaultDNSRecord(
                                ipv6Address ? DNSType.AAAA : DNSType.A,
                                this.getCloudNetConfig().getIdentity().getUniqueId() + "."
                                        + cloudflareConfigurationEntry.getDomainName(),
                                hostAddress,
                                new JsonObject()
                        )
                );

                if (response.getFirst() < 400) {
                    CloudNetDriver.getInstance().getLogger().info(LanguageManager.getMessage("module-cloudflare-create-dns-record-for-service")
                            .replace("%service%", this.getCloudNet().getConfig().getIdentity().getUniqueId())
                            .replace("%domain%", cloudflareConfigurationEntry.getDomainName())
                            .replace("%recordId%", response.getSecond().getDocument("result").getString("id"))
                    );
                }
            }
        }
    }

    @ModuleTask(order = 124, event = ModuleLifeCycle.STARTED)
    public void registerListeners() {
        if (CloudflareAPI.getInstance() == null) {
            return;
        }

        this.registerListener(new CloudflareStartAndStopListener());
    }

    @ModuleTask(order = 123, event = ModuleLifeCycle.STARTED)
    public void registerHttpHandlers() {
        if (CloudflareAPI.getInstance() == null) {
            return;
        }

        this.getHttpServer().registerHandler("/api/v1/modules/cloudflare/config",
                new V1CloudflareConfigurationHttpHandler("cloudnet.http.v1.modules.cloudflare.config"));
    }

    public void updateConfiguration(CloudflareConfiguration cloudflareConfiguration) {
        this.cloudflareConfiguration = cloudflareConfiguration;

        this.getConfig().append("config", cloudflareConfiguration);
        this.saveConfig();
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STOPPED)
    public void closeCloudflareAPI() {
        if (CloudflareAPI.getInstance() != null) {
            try {
                CloudflareAPI.getInstance().close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @ModuleTask(order = 64, event = ModuleLifeCycle.STOPPED)
    public void removeRecordsOnDelete() {
        if (this.cloudflareConfiguration.getEntries().stream().noneMatch(CloudflareConfigurationEntry::isEnabled)) {
            return;
        }

        for (Map.Entry<String, Pair<String, JsonDocument>> entry : CloudflareAPI.getInstance().getCreatedRecords().entrySet()) {
            CloudflareAPI.getInstance().deleteRecord(
                    entry.getValue().getSecond().getString("email"),
                    entry.getValue().getSecond().get("authenticationMethod", CloudflareConfigurationEntry.AuthenticationMethod.class),
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