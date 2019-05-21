package de.dytanic.cloudnet.http;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import de.dytanic.cloudnet.driver.network.http.HttpCookie;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

public final class V1HttpSession {

    private static final String COOKIE_NAME = "CloudNet-REST_V1-Session." + new Random().nextInt();

    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;

    private final Collection<SessionEntry> entries = Iterables.newCopyOnWriteArrayList();

    public boolean auth(IHttpContext context) throws Exception
    {
        if (isAuthorized(context))
            logout(context);

        if (!context.request().hasHeader("Authorization")) return false;

        String[] typeAndCredentials = context.request().header("Authorization").split(" ");

        if (typeAndCredentials.length != 2 || !typeAndCredentials[0].equalsIgnoreCase("Basic")) return false;

        String[] credentials = new String(Base64.getDecoder().decode(typeAndCredentials[1]), StandardCharsets.UTF_8).split(":");
        if (credentials.length != 2) return false;

        List<IPermissionUser> permissionUsers = CloudNet.getInstance().getPermissionManagement().getUser(credentials[0]);
        IPermissionUser permissionUser = Iterables.first(permissionUsers, new Predicate<IPermissionUser>() {
            @Override
            public boolean test(IPermissionUser iPermissionUser)
            {
                return iPermissionUser.checkPassword(credentials[1]);
            }
        });

        if (permissionUser == null) return false;

        SessionEntry sessionEntry = new SessionEntry(
            System.nanoTime(),
            System.currentTimeMillis(),
            context.channel().clientAddress().getHost(),
            UUID.randomUUID().toString(),
            permissionUser.getUniqueId().toString()
        );

        context.addCookie(new HttpCookie(
            COOKIE_NAME,
            createKey(sessionEntry, context),
            null,
            "/",
            sessionEntry.lastUsageMillis + EXPIRE_TIME))
            .response()
            .statusCode(200)
        ;

        entries.add(sessionEntry);
        return true;
    }

    public boolean isAuthorized(IHttpContext context) throws Exception
    {
        if (!context.hasCookie(COOKIE_NAME))
            return false;

        HttpCookie httpCookie = context.cookie(COOKIE_NAME);

        SessionEntry sessionEntry = getValidSessionEntry(httpCookie.getValue(), context);
        if (sessionEntry == null) return false;

        if ((sessionEntry.lastUsageMillis + EXPIRE_TIME) < System.currentTimeMillis())
        {
            logout(context);
            return false;
        }

        sessionEntry.lastUsageMillis = System.currentTimeMillis();
        httpCookie.setMaxAge(sessionEntry.lastUsageMillis + EXPIRE_TIME);
        return true;
    }

    public SessionEntry getValidSessionEntry(String cookieValue, IHttpContext context) throws Exception
    {
        if (cookieValue == null || context == null) return null;

        for (SessionEntry entry : this.entries)
            if (cookieValue.equals(createKey(entry, context)))
                return entry;

        return null;
    }

    public void logout(IHttpContext context) throws Exception
    {
        Validate.checkNotNull(context);

        SessionEntry sessionEntry = getValidSessionEntry(getCookieValue(context), context);
        if (sessionEntry != null)
            this.entries.remove(sessionEntry);

        context.removeCookie(COOKIE_NAME);
    }

    public IPermissionUser getUser(IHttpContext context) throws Exception
    {
        Validate.checkNotNull(context);

        SessionEntry sessionEntry = getValidSessionEntry(getCookieValue(context), context);

        return getUser(sessionEntry, context);
    }

    private IPermissionUser getUser(SessionEntry sessionEntry, IHttpContext context) throws Exception
    {
        if (sessionEntry == null || context == null) return null;

        return CloudNet.getInstance().getPermissionManagement().getUser(UUID.fromString(sessionEntry.userUniqueId));
    }

    private String getCookieValue(IHttpContext context) throws Exception
    {
        HttpCookie httpCookie = context.cookie(COOKIE_NAME);

        if (httpCookie != null)
            return httpCookie.getValue();
        else
            return null;
    }

    private String createKey(SessionEntry sessionEntry, IHttpContext context)
    {
        return Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(sessionEntry.creationTime +
            ":" +
            context.channel().clientAddress().getHost() +
            "#" +
            sessionEntry.uniqueId +
            "#" +
            sessionEntry.userUniqueId
        ));
    }

    @AllArgsConstructor
    public class SessionEntry {

        long creationTime, lastUsageMillis;

        String host, uniqueId, userUniqueId;

    }
}