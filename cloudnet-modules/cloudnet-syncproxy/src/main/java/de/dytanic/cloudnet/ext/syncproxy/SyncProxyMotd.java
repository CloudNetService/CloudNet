package de.dytanic.cloudnet.ext.syncproxy;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class SyncProxyMotd {

    protected String firstLine, secondLine;

    protected boolean autoSlot;

    protected int autoSlotMaxPlayersDistance;

    protected String[] playerInfo;

    protected String protocolText;

    public SyncProxyMotd(String firstLine, String secondLine, boolean autoSlot, int autoSlotMaxPlayersDistance, String[] playerInfo, String protocolText) {
        this.firstLine = firstLine;
        this.secondLine = secondLine;
        this.autoSlot = autoSlot;
        this.autoSlotMaxPlayersDistance = autoSlotMaxPlayersDistance;
        this.playerInfo = playerInfo;
        this.protocolText = protocolText;
    }

    public SyncProxyMotd() {
    }

    public String getFirstLine() {
        return this.firstLine;
    }

    public String getSecondLine() {
        return this.secondLine;
    }

    public boolean isAutoSlot() {
        return this.autoSlot;
    }

    public int getAutoSlotMaxPlayersDistance() {
        return this.autoSlotMaxPlayersDistance;
    }

    public String[] getPlayerInfo() {
        return this.playerInfo;
    }

    public String getProtocolText() {
        return this.protocolText;
    }

    public void setFirstLine(String firstLine) {
        this.firstLine = firstLine;
    }

    public void setSecondLine(String secondLine) {
        this.secondLine = secondLine;
    }

    public void setAutoSlot(boolean autoSlot) {
        this.autoSlot = autoSlot;
    }

    public void setAutoSlotMaxPlayersDistance(int autoSlotMaxPlayersDistance) {
        this.autoSlotMaxPlayersDistance = autoSlotMaxPlayersDistance;
    }

    public void setPlayerInfo(String[] playerInfo) {
        this.playerInfo = playerInfo;
    }

    public void setProtocolText(String protocolText) {
        this.protocolText = protocolText;
    }

}