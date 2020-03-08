package de.dytanic.cloudnet.ext.report.web;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.IHttpServer;
import de.dytanic.cloudnet.ext.report.web.remote.WebReport;
import de.dytanic.cloudnet.ext.report.web.local.CloudNetWebReportAssetsHandler;
import de.dytanic.cloudnet.ext.report.web.local.CloudNetWebReportHandler;
import de.dytanic.cloudnet.ext.report.web.remote.WebReportUploadResult;
import de.dytanic.cloudnet.ext.report.web.type.ReportHandlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class WebReportProvider {

    private String postUrl;
    private boolean webEnabled = false;

    public WebReportProvider(String postUrl) {
        this.postUrl = postUrl.endsWith("/") ? postUrl : postUrl + "/";
    }

    public void registerHttpHandlers(IHttpServer httpServer) {
        httpServer.redirect("/report", "/report/index.html");
        httpServer.get("/report/{type}", new CloudNetWebReportHandler());
        CloudNetWebReportAssetsHandler assetsHandler = new CloudNetWebReportAssetsHandler();
        httpServer.get(assetsHandler.getPath(), assetsHandler);
    }

    public boolean isWebEnabled() {
        return this.webEnabled;
    }

    public void setWebEnabled(boolean webEnabled) {
        this.webEnabled = webEnabled;
    }

    public WebReport createReport() {
        Map<String, Map<String, String>> replacementsByFile = new HashMap<>();
        for (String file : ReportHandlers.getFiles()) {
            Map<String, String> replacements = new HashMap<>();
            ReportHandlers.loadReplacements(file).forEach((key, value) -> replacements.put(key, String.valueOf(value)));
            replacementsByFile.put(file, replacements);
        }
        return new WebReport(replacementsByFile);
    }

    public WebReportUploadResult uploadReport(WebReport webReport) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(this.postUrl).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        connection.connect();

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(JsonDocument.newDocument(webReport).toByteArray());
        }

        JsonDocument result;
        try (InputStream inputStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream()) {
            result = JsonDocument.newDocument().read(inputStream);
        }

        connection.disconnect();

        if (result.contains("error")) {
            if (connection.getResponseCode() == 429) {
                long minutesLeft = (connection.getHeaderFieldLong("X-RateLimit-End", System.currentTimeMillis()) - System.currentTimeMillis()) / 1000 / 60;
                throw new IllegalStateException(result.getString("error") + " (time left: " + minutesLeft + " minutes)");
            }
            throw new IllegalStateException("Received error while uploading report: " + result.getString("error"));
        }

        return new WebReportUploadResult(
                this.postUrl + result.getString("key"),
                result.getLong("timeout")
        );
    }

}
