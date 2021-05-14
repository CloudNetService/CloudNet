package de.dytanic.cloudnet.http.v2;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.conf.IConfiguration;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpHandler;
import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import de.dytanic.cloudnet.driver.network.http.IHttpResponse;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class V2HttpHandler implements IHttpHandler {

    protected static final V2HttpAuthentication DEFAULT_AUTH = new V2HttpAuthentication();

    protected final String requiredPermission;
    protected final String[] supportedRequestMethods;
    protected final String supportedRequestMethodsString;

    protected V2HttpAuthentication authentication;

    public V2HttpHandler(String requiredPermission, String... supportedRequestMethods) {
        this(requiredPermission, DEFAULT_AUTH, supportedRequestMethods);
    }

    public V2HttpHandler(String requiredPermission, V2HttpAuthentication authentication, String... supportedRequestMethods) {
        this.requiredPermission = requiredPermission;
        this.authentication = authentication;

        this.supportedRequestMethods = supportedRequestMethods;
        // needed to use a binary search later
        Arrays.sort(this.supportedRequestMethods);
        this.supportedRequestMethodsString = supportedRequestMethods.length == 0
                ? "*" : String.join(", ", supportedRequestMethods);
    }

    @Override
    public void handle(String path, IHttpContext context) throws Exception {
        if (context.request().method().equalsIgnoreCase("OPTIONS")) {
            this.sendOptions(context);
        } else {
            if (this.supportedRequestMethods.length > 0
                    && Arrays.binarySearch(this.supportedRequestMethods, context.request().method().toUpperCase()) < 0) {
                this.response(context, HttpResponseCode.HTTP_BAD_METHOD)
                        .header("Allow", this.supportedRequestMethodsString)
                        .context()
                        .cancelNext(true)
                        .closeAfter();
            } else if (context.request().hasHeader("Authorization")) {
                // try the more often used bearer auth first
                V2HttpAuthentication.LoginResult<HttpSession> session = this.authentication.handleBearerLoginRequest(context.request());
                if (session.isSuccess()) {
                    if (this.testPermission(session.getResult().getUser(), context.request())) {
                        this.handleBearerAuthorized(path, context, session.getResult());
                    } else {
                        this.send403(context, String.format("Required permission %s not set", this.requiredPermission));
                    }
                    return;
                } else if (session.hasErrorMessage()) {
                    this.send403(context, session.getErrorMessage());
                    return;
                }
                // try the basic auth method
                V2HttpAuthentication.LoginResult<IPermissionUser> user = this.authentication.handleBasicLoginRequest(context.request());
                if (user.isSuccess()) {
                    if (this.testPermission(user.getResult(), context.request())) {
                        this.handleBasicAuthorized(path, context, user.getResult());
                    } else {
                        this.send403(context, String.format("Required permission %s not set", this.requiredPermission));
                    }
                    return;
                } else if (user.hasErrorMessage()) {
                    this.send403(context, user.getErrorMessage());
                    return;
                }
                // send an unauthorized response
                this.send403(context, "No supported authentication method provided. Supported: Basic, Bearer");
            } else {
                // there was no authorization given, try without one
                this.handleUnauthorized(path, context);
            }
        }
    }

    protected void handleUnauthorized(String path, IHttpContext context) throws Exception {
        this.send403(context, "Authentication required");
    }

    protected void handleBasicAuthorized(String path, IHttpContext context, IPermissionUser user) throws Exception {
    }

    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) throws Exception {
    }

    protected boolean testPermission(@NotNull IPermissionUser user, @NotNull IHttpRequest request) {
        if (this.requiredPermission == null || this.requiredPermission.isEmpty()) {
            return true;
        } else {
            return CloudNetDriver.getInstance().getPermissionManagement().hasPermission(user,
                    this.requiredPermission + '.' + request.method().toLowerCase());
        }
    }

    protected void send403(IHttpContext context, String reason) {
        context.response()
                .statusCode(HttpResponseCode.HTTP_FORBIDDEN)
                .header("Content-Type", "application/json")
                .body(this.failure().append("reason", reason).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void sendOptions(IHttpContext context) {
        context
                .cancelNext(true)
                .response()
                .statusCode(HttpResponseCode.HTTP_NO_CONTENT)
                .header("Access-Control-Max-Age", "3600")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "*")
                .header("Access-Control-Expose-Headers", "Accept, Origin, if-none-match, Access-Control-Allow-Headers, " +
                        "Access-Control-Allow-Origin, Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", this.supportedRequestMethodsString);
    }

    protected IHttpResponse ok(IHttpContext context) {
        return this.response(context, HttpResponseCode.HTTP_OK);
    }

    protected IHttpResponse badRequest(IHttpContext context) {
        return this.response(context, HttpResponseCode.HTTP_BAD_REQUEST);
    }

    protected IHttpResponse notFound(IHttpContext context) {
        return this.response(context, HttpResponseCode.HTTP_NOT_FOUND);
    }

    protected IHttpResponse response(IHttpContext context, int statusCode) {
        return context.response()
                .statusCode(statusCode)
                .header("Content-Type", "application/json")
                .header("Access-Control-Allow-Origin", "*");
    }

    protected JsonDocument body(IHttpRequest request) {
        return JsonDocument.newDocument(new String(request.body(), StandardCharsets.UTF_8));
    }

    protected JsonDocument success() {
        return JsonDocument.newDocument("success", true);
    }

    protected JsonDocument failure() {
        return JsonDocument.newDocument("success", false);
    }

    protected CloudNet getCloudNet() {
        return CloudNet.getInstance();
    }

    protected IConfiguration getConfiguration() {
        return this.getCloudNet().getConfig();
    }
}
