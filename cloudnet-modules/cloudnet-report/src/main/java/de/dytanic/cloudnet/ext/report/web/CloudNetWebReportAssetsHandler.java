package de.dytanic.cloudnet.ext.report.web;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.MethodHttpHandlerAdapter;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;

import java.io.InputStream;

public class CloudNetWebReportAssetsHandler extends MethodHttpHandlerAdapter {

    private final String path = "/report/assets/*";
    private final String replacePath = "/report/assets/";

    public String getPath() {
        return this.path;
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        if (path.contains("..")) {
            return;
        }

        path = path.substring(this.replacePath.length());

        try (InputStream inputStream = CloudNetReportModule.class.getClassLoader().getResourceAsStream("web/assets/" + path)) {
            if (inputStream != null) {
                context.response()
                        .body(FileUtils.toByteArray(inputStream))
                        .header("Content-Type", path.endsWith(".css") ? "text/css" : path.endsWith(".js") ? "application/js" : "text/plain")
                        .statusCode(200);
            }
        }

    }
}
