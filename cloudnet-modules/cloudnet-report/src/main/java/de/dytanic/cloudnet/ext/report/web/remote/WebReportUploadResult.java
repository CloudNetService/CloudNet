package de.dytanic.cloudnet.ext.report.web.remote;

public class WebReportUploadResult {

    private String url;
    private long timeout;

    public WebReportUploadResult(String url, long timeout) {
        this.url = url;
        this.timeout = timeout;
    }

    public String getUrl() {
        return this.url;
    }

    public long getTimeout() {
        return this.timeout;
    }
}
