package de.dytanic.cloudnet.ext.syncproxy;

public class SyncProxyTabList {

    protected String header;

    protected String footer;

    public SyncProxyTabList(String header, String footer) {
        this.header = header;
        this.footer = footer;
    }

    public SyncProxyTabList() {
    }

    public String getHeader() {
        return this.header;
    }

    public String getFooter() {
        return this.footer;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SyncProxyTabList)) return false;
        final SyncProxyTabList other = (SyncProxyTabList) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$header = this.getHeader();
        final Object other$header = other.getHeader();
        if (this$header == null ? other$header != null : !this$header.equals(other$header)) return false;
        final Object this$footer = this.getFooter();
        final Object other$footer = other.getFooter();
        if (this$footer == null ? other$footer != null : !this$footer.equals(other$footer)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SyncProxyTabList;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $header = this.getHeader();
        result = result * PRIME + ($header == null ? 43 : $header.hashCode());
        final Object $footer = this.getFooter();
        result = result * PRIME + ($footer == null ? 43 : $footer.hashCode());
        return result;
    }

    public String toString() {
        return "SyncProxyTabList(header=" + this.getHeader() + ", footer=" + this.getFooter() + ")";
    }
}