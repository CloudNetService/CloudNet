package de.dytanic.cloudnet.ext.signs.node.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.signs.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.ext.signs.node.CloudNetSignsModule;
import de.dytanic.cloudnet.http.V1HttpHandler;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class V1SignConfigurationHttpHandler extends V1HttpHandler {

    public V1SignConfigurationHttpHandler(String permission) {
        super(permission);
    }

    @Override
    public void handleOptions(String path, IHttpContext context) throws Exception {
        this.sendOptions(context, "GET, POST");
    }

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {
        context
                .response()
                .statusCode(HttpResponseCode.HTTP_OK)
                .header("Content-Type", "application/json")
                .body(GSON.toJson(CloudNetSignsModule.getInstance().getSignConfiguration()))
                .context()
                .closeAfter(true)
                .cancelNext()
        ;
    }

    @Override
    public void handlePost(String path, IHttpContext context) throws Exception {
        try {
            if (context.request().body().length > 0) {
                SignConfiguration signConfiguration = GSON.fromJson(context.request().bodyAsString(), SignConfiguration.TYPE);

                if (signConfiguration != null) {
                    CloudNetSignsModule.getInstance().setSignConfiguration(signConfiguration);
                    SignConfigurationReaderAndWriter.write(signConfiguration, CloudNetSignsModule.getInstance().getConfigurationFile());

                    CloudNetDriver.getInstance().sendChannelMessage(
                            SignConstants.SIGN_CHANNEL_NAME,
                            SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION,
                            new JsonDocument("signConfiguration", signConfiguration)
                    );

                    context
                            .response()
                            .statusCode(HttpResponseCode.HTTP_OK)
                            .header("Content-Type", "application")
                            .body(new JsonDocument("success", true).toByteArray())
                            .context()
                            .closeAfter(true)
                            .cancelNext()
                    ;
                }
            }

        } catch (Exception ex) {

            try (StringWriter writer = new StringWriter();
                 PrintWriter printWriter = new PrintWriter(writer)) {
                ex.printStackTrace(printWriter);
                this.send400Response(context, writer.getBuffer().toString());
            }
        }
    }
}