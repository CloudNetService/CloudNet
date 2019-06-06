package de.dytanic.cloudnet.ext.signs;

public class SignLayout {

    protected String[] lines;

    protected String blockType;

    protected int subId;

    public SignLayout(String[] lines, String blockType, int subId) {
        this.lines = lines;
        this.blockType = blockType;
        this.subId = subId;
    }

    public SignLayout() {
    }

    public String[] getLines() {
        return this.lines;
    }

    public String getBlockType() {
        return this.blockType;
    }

    public int getSubId() {
        return this.subId;
    }

    public void setLines(String[] lines) {
        this.lines = lines;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public void setSubId(int subId) {
        this.subId = subId;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SignLayout)) return false;
        final SignLayout other = (SignLayout) o;
        if (!other.canEqual((Object) this)) return false;
        if (!java.util.Arrays.deepEquals(this.getLines(), other.getLines())) return false;
        final Object this$blockType = this.getBlockType();
        final Object other$blockType = other.getBlockType();
        if (this$blockType == null ? other$blockType != null : !this$blockType.equals(other$blockType)) return false;
        if (this.getSubId() != other.getSubId()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SignLayout;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getLines());
        final Object $blockType = this.getBlockType();
        result = result * PRIME + ($blockType == null ? 43 : $blockType.hashCode());
        result = result * PRIME + this.getSubId();
        return result;
    }

    public String toString() {
        return "SignLayout(lines=" + java.util.Arrays.deepToString(this.getLines()) + ", blockType=" + this.getBlockType() + ", subId=" + this.getSubId() + ")";
    }
}