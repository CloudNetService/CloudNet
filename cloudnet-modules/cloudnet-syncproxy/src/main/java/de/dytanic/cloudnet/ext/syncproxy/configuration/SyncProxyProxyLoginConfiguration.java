package de.dytanic.cloudnet.ext.syncproxy.configuration;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode
public class SyncProxyProxyLoginConfiguration {

    protected String targetGroup;

    protected boolean maintenance;

    protected int maxPlayers;

    protected List<String> whitelist;

    protected List<SyncProxyMotd> motds;

    protected List<SyncProxyMotd> maintenanceMotds;

    public SyncProxyProxyLoginConfiguration(String targetGroup, boolean maintenance, int maxPlayers, List<String> whitelist, List<SyncProxyMotd> motds, List<SyncProxyMotd> maintenanceMotds) {
        this.targetGroup = targetGroup;
        this.maintenance = maintenance;
        this.maxPlayers = maxPlayers;
        this.whitelist = whitelist;
        this.motds = motds;
        this.maintenanceMotds = maintenanceMotds;
    }

    public SyncProxyProxyLoginConfiguration() {
    }

    public String getTargetGroup() {
        return this.targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public boolean isMaintenance() {
        return this.maintenance;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public List<String> getWhitelist() {
        return this.whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    public List<SyncProxyMotd> getMotds() {
        return this.motds;
    }

    public void setMotds(List<SyncProxyMotd> motds) {
        this.motds = motds;
    }

    public List<SyncProxyMotd> getMaintenanceMotds() {
        return this.maintenanceMotds;
    }

    public void setMaintenanceMotds(List<SyncProxyMotd> maintenanceMotds) {
        this.maintenanceMotds = maintenanceMotds;
    }

}