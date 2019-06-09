package de.dytanic.cloudnet.ext.syncproxy;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
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

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return this.footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

}