package eu.cloudnetservice.cloudnet.ext.labymod;

import eu.cloudnetservice.cloudnet.ext.labymod.player.DiscordJoinMatchConfig;
import eu.cloudnetservice.cloudnet.ext.labymod.player.ServiceDisplay;

public class LabyModConfiguration {

    private boolean enabled;
    private ServiceDisplay discordRPC;
    private DiscordJoinMatchConfig discordJoinMatch;
    private ServiceDisplay gameModeSwitchMessages;
    private String loginDomain;

    public LabyModConfiguration(boolean enabled, ServiceDisplay discordRPC, DiscordJoinMatchConfig discordJoinMatch, ServiceDisplay gameModeSwitchMessages, String loginDomain) {
        this.enabled = enabled;
        this.discordRPC = discordRPC;
        this.discordJoinMatch = discordJoinMatch;
        this.gameModeSwitchMessages = gameModeSwitchMessages;
        this.loginDomain = loginDomain;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ServiceDisplay getDiscordRPC() {
        return this.discordRPC;
    }

    public void setDiscordRPC(ServiceDisplay discordRPC) {
        this.discordRPC = discordRPC;
    }

    public ServiceDisplay getGameModeSwitchMessages() {
        return this.gameModeSwitchMessages;
    }

    public void setGameModeSwitchMessages(ServiceDisplay gameModeSwitchMessages) {
        this.gameModeSwitchMessages = gameModeSwitchMessages;
    }

    public DiscordJoinMatchConfig getDiscordJoinMatch() {
        return this.discordJoinMatch;
    }

    public void setDiscordJoinMatch(DiscordJoinMatchConfig discordJoinMatch) {
        this.discordJoinMatch = discordJoinMatch;
    }

    public String getLoginDomain() {
        return this.loginDomain;
    }

    public void setLoginDomain(String loginDomain) {
        this.loginDomain = loginDomain;
    }

}
