package de.dytanic.cloudnet.ext.syncproxy;

import java.util.List;

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

    public boolean isMaintenance() {
        return this.maintenance;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public List<String> getWhitelist() {
        return this.whitelist;
    }

    public List<SyncProxyMotd> getMotds() {
        return this.motds;
    }

    public List<SyncProxyMotd> getMaintenanceMotds() {
        return this.maintenanceMotds;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    public void setMotds(List<SyncProxyMotd> motds) {
        this.motds = motds;
    }

    public void setMaintenanceMotds(List<SyncProxyMotd> maintenanceMotds) {
        this.maintenanceMotds = maintenanceMotds;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SyncProxyProxyLoginConfiguration)) return false;
        final SyncProxyProxyLoginConfiguration other = (SyncProxyProxyLoginConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$targetGroup = this.getTargetGroup();
        final Object other$targetGroup = other.getTargetGroup();
        if (this$targetGroup == null ? other$targetGroup != null : !this$targetGroup.equals(other$targetGroup))
            return false;
        if (this.isMaintenance() != other.isMaintenance()) return false;
        if (this.getMaxPlayers() != other.getMaxPlayers()) return false;
        final Object this$whitelist = this.getWhitelist();
        final Object other$whitelist = other.getWhitelist();
        if (this$whitelist == null ? other$whitelist != null : !this$whitelist.equals(other$whitelist)) return false;
        final Object this$motds = this.getMotds();
        final Object other$motds = other.getMotds();
        if (this$motds == null ? other$motds != null : !this$motds.equals(other$motds)) return false;
        final Object this$maintenanceMotds = this.getMaintenanceMotds();
        final Object other$maintenanceMotds = other.getMaintenanceMotds();
        if (this$maintenanceMotds == null ? other$maintenanceMotds != null : !this$maintenanceMotds.equals(other$maintenanceMotds))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SyncProxyProxyLoginConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $targetGroup = this.getTargetGroup();
        result = result * PRIME + ($targetGroup == null ? 43 : $targetGroup.hashCode());
        result = result * PRIME + (this.isMaintenance() ? 79 : 97);
        result = result * PRIME + this.getMaxPlayers();
        final Object $whitelist = this.getWhitelist();
        result = result * PRIME + ($whitelist == null ? 43 : $whitelist.hashCode());
        final Object $motds = this.getMotds();
        result = result * PRIME + ($motds == null ? 43 : $motds.hashCode());
        final Object $maintenanceMotds = this.getMaintenanceMotds();
        result = result * PRIME + ($maintenanceMotds == null ? 43 : $maintenanceMotds.hashCode());
        return result;
    }

    public String toString() {
        return "SyncProxyProxyLoginConfiguration(targetGroup=" + this.getTargetGroup() + ", maintenance=" + this.isMaintenance() + ", maxPlayers=" + this.getMaxPlayers() + ", whitelist=" + this.getWhitelist() + ", motds=" + this.getMotds() + ", maintenanceMotds=" + this.getMaintenanceMotds() + ")";
    }
}