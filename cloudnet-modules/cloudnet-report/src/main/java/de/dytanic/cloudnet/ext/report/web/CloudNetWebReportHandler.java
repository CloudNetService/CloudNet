package de.dytanic.cloudnet.ext.report.web;

import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.ext.report.CloudNetReportModule;
import de.dytanic.cloudnet.ext.report.web.type.ReportHandlers;

public class CloudNetWebReportHandler implements IHttpHandler {

    @Override
    public void handle(String path, IHttpContext context) throws Exception {
        if (!CloudNetReportModule.getInstance().isWebEnabled()) {
            context.response()
                    .statusCode(404)
                    .context()
                    .cancelNext();
            return;
        }

        String type = context.request().pathParameters().get("type");
        if (type == null || type.isEmpty()) {
            type = "index.html";
        }
        String response = ReportHandlers.loadTypeContents(type, context);

        if (response == null) {
            context.response()
                    .statusCode(404)
                    .context()
                    .cancelNext();
            return;
        }

        context.response()
                .header("Content-Type", "text/html")
                .statusCode(200)
                .body(response)
                .context();
    }
}
