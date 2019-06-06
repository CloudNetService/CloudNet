package de.dytanic.cloudnet.driver.module;

public class ModuleUpdateServiceConfiguration {

    protected boolean autoInstall;

    protected String url, currentVersion, infoMessage;

    public ModuleUpdateServiceConfiguration(boolean autoInstall, String url, String currentVersion, String infoMessage) {
        this.autoInstall = autoInstall;
        this.url = url;
        this.currentVersion = currentVersion;
        this.infoMessage = infoMessage;
    }

    public ModuleUpdateServiceConfiguration() {
    }

    public boolean isAutoInstall() {
        return this.autoInstall;
    }

    public String getUrl() {
        return this.url;
    }

    public String getCurrentVersion() {
        return this.currentVersion;
    }

    public String getInfoMessage() {
        return this.infoMessage;
    }

    public void setAutoInstall(boolean autoInstall) {
        this.autoInstall = autoInstall;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void setInfoMessage(String infoMessage) {
        this.infoMessage = infoMessage;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ModuleUpdateServiceConfiguration)) return false;
        final ModuleUpdateServiceConfiguration other = (ModuleUpdateServiceConfiguration) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isAutoInstall() != other.isAutoInstall()) return false;
        final Object this$url = this.getUrl();
        final Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) return false;
        final Object this$currentVersion = this.getCurrentVersion();
        final Object other$currentVersion = other.getCurrentVersion();
        if (this$currentVersion == null ? other$currentVersion != null : !this$currentVersion.equals(other$currentVersion))
            return false;
        final Object this$infoMessage = this.getInfoMessage();
        final Object other$infoMessage = other.getInfoMessage();
        if (this$infoMessage == null ? other$infoMessage != null : !this$infoMessage.equals(other$infoMessage))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ModuleUpdateServiceConfiguration;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isAutoInstall() ? 79 : 97);
        final Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        final Object $currentVersion = this.getCurrentVersion();
        result = result * PRIME + ($currentVersion == null ? 43 : $currentVersion.hashCode());
        final Object $infoMessage = this.getInfoMessage();
        result = result * PRIME + ($infoMessage == null ? 43 : $infoMessage.hashCode());
        return result;
    }

    public String toString() {
        return "ModuleUpdateServiceConfiguration(autoInstall=" + this.isAutoInstall() + ", url=" + this.getUrl() + ", currentVersion=" + this.getCurrentVersion() + ", infoMessage=" + this.getInfoMessage() + ")";
    }
}