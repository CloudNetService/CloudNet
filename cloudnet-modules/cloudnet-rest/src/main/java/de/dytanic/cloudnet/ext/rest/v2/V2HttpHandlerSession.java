package de.dytanic.cloudnet.ext.rest.v2;

import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.http.v2.HttpSession;
import de.dytanic.cloudnet.http.v2.V2HttpHandler;

import java.util.concurrent.TimeUnit;

public class V2HttpHandlerSession extends V2HttpHandler {

    public V2HttpHandlerSession() {
        super(null, "POST");
    }

    @Override
    protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
        if (path.startsWith("/api/v2/session/logout")) {
            this.handleLogout(context, session);
        } else if (path.startsWith("/api/v2/session/refresh")) {
            this.handleRefresh(context, session);
        } else {
            this.response(context, HttpResponseCode.HTTP_NOT_FOUND).context().closeAfter(true).cancelNext();
        }
    }

    protected void handleRefresh(IHttpContext context, HttpSession session) {
        String jwt = this.authentication.refreshJwt(session, TimeUnit.HOURS.toMillis(1));
        this.ok(context)
                .body(this.success().append("token", jwt).append("uniqueId", session.getUser().getUniqueId()).toByteArray())
                .context()
                .closeAfter(true)
                .cancelNext();
    }

    protected void handleLogout(IHttpContext context, HttpSession session) {
        if (this.authentication.expireSession(session)) {
            this.ok(context).body(this.success().toByteArray()).context().closeAfter(true).cancelNext();
        } else {
            this.send403(context, "Unable to close unknown session");
        }
    }
}
