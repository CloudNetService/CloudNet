package de.dytanic.cloudnet.ext.syncproxy;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SyncProxyMotd)) return false;
        final SyncProxyMotd other = (SyncProxyMotd) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$firstLine = this.getFirstLine();
        final Object other$firstLine = other.getFirstLine();
        if (this$firstLine == null ? other$firstLine != null : !this$firstLine.equals(other$firstLine)) return false;
        final Object this$secondLine = this.getSecondLine();
        final Object other$secondLine = other.getSecondLine();
        if (this$secondLine == null ? other$secondLine != null : !this$secondLine.equals(other$secondLine))
            return false;
        if (this.isAutoSlot() != other.isAutoSlot()) return false;
        if (this.getAutoSlotMaxPlayersDistance() != other.getAutoSlotMaxPlayersDistance()) return false;
        if (!java.util.Arrays.deepEquals(this.getPlayerInfo(), other.getPlayerInfo())) return false;
        final Object this$protocolText = this.getProtocolText();
        final Object other$protocolText = other.getProtocolText();
        if (this$protocolText == null ? other$protocolText != null : !this$protocolText.equals(other$protocolText))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SyncProxyMotd;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $firstLine = this.getFirstLine();
        result = result * PRIME + ($firstLine == null ? 43 : $firstLine.hashCode());
        final Object $secondLine = this.getSecondLine();
        result = result * PRIME + ($secondLine == null ? 43 : $secondLine.hashCode());
        result = result * PRIME + (this.isAutoSlot() ? 79 : 97);
        result = result * PRIME + this.getAutoSlotMaxPlayersDistance();
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getPlayerInfo());
        final Object $protocolText = this.getProtocolText();
        result = result * PRIME + ($protocolText == null ? 43 : $protocolText.hashCode());
        return result;
    }

    public String toString() {
        return "SyncProxyMotd(firstLine=" + this.getFirstLine() + ", secondLine=" + this.getSecondLine() + ", autoSlot=" + this.isAutoSlot() + ", autoSlotMaxPlayersDistance=" + this.getAutoSlotMaxPlayersDistance() + ", playerInfo=" + java.util.Arrays.deepToString(this.getPlayerInfo()) + ", protocolText=" + this.getProtocolText() + ")";
    }
}