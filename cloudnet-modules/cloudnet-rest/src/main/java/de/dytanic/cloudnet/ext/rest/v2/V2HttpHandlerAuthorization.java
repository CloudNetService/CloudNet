package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;

import java.util.concurrent.TimeUnit;

public class V2HttpHandlerAuthorization extends V2HttpHandler {

    public V2HttpHandlerAuthorization() {
        super(null, "POST");
    }

    @Override
    protected void handleUnauthorized(String path, IHttpContext context) {
        this.response(context, HttpResponseCode.HTTP_UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"CloudNet Rest\"")
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    @Override
    protected void handleBasicAuthorized(String path, IHttpContext context, IPermissionUser user) {
        String jwt = this.authentication.createJwt(user, TimeUnit.HOURS.toMillis(1)); // todo: configurable
        this.ok(context)
                .body(this.success().append("token", jwt).append("id", user.getUniqueId()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
        this.ok(context)
                .body(this.success().append("id", session.getUser().getUniqueId()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }
}
