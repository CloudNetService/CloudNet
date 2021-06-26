/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.http;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import de.dytanic.cloudnet.driver.network.http.HttpCookie;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class V1HttpSession {

  private static final Pattern BASE64_PATTERN = Pattern
    .compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
  private static final String COOKIE_NAME = "CloudNet-REST_V1-Session." + new Random().nextInt();

  private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;

  private final Collection<SessionEntry> entries = new CopyOnWriteArrayList<>();

  public boolean auth(IHttpContext context) throws Exception {
    if (this.isAuthorized(context)) {
      this.logout(context);
    }

    if (!context.request().hasHeader("Authorization")) {
      return false;
    }

    String[] typeAndCredentials = context.request().header("Authorization").split(" ");

    if (typeAndCredentials.length != 2 || !typeAndCredentials[0].equalsIgnoreCase("Basic")) {
      return false;
    }

    if (!BASE64_PATTERN.matcher(typeAndCredentials[1]).matches()) {
      return false;
    }

    String[] credentials = new String(Base64.getDecoder().decode(typeAndCredentials[1]), StandardCharsets.UTF_8)
      .split(":");
    if (credentials.length != 2) {
      return false;
    }

    List<IPermissionUser> permissionUsers = CloudNet.getInstance().getPermissionManagement().getUsers(credentials[0]);
    IPermissionUser permissionUser = permissionUsers.stream()
      .filter(user -> user.checkPassword(credentials[1])).findFirst().orElse(null);

    if (permissionUser == null) {
      return false;
    }

    SessionEntry sessionEntry = new SessionEntry(
      System.nanoTime(),
      System.currentTimeMillis(),
      UUID.randomUUID().toString(),
      permissionUser.getUniqueId().toString()
    );

    context.addCookie(new HttpCookie(
      COOKIE_NAME,
      this.createKey(sessionEntry, context),
      null,
      "/",
      sessionEntry.lastUsageMillis + EXPIRE_TIME))
      .response()
      .statusCode(200)
    ;

    this.entries.add(sessionEntry);
    return true;
  }

  public boolean isAuthorized(IHttpContext context) {
    if (!context.hasCookie(COOKIE_NAME)) {
      return false;
    }

    HttpCookie httpCookie = context.cookie(COOKIE_NAME);

    SessionEntry sessionEntry = this.getValidSessionEntry(httpCookie.getValue(), context);
    if (sessionEntry == null) {
      return false;
    }

    if ((sessionEntry.lastUsageMillis + EXPIRE_TIME) < System.currentTimeMillis()) {
      this.logout(context);
      return false;
    }

    sessionEntry.lastUsageMillis = System.currentTimeMillis();
    httpCookie.setMaxAge(sessionEntry.lastUsageMillis + EXPIRE_TIME);
    return true;
  }

  public SessionEntry getValidSessionEntry(String cookieValue, IHttpContext context) {
    if (cookieValue == null || context == null) {
      return null;
    }

    for (SessionEntry entry : this.entries) {
      if (cookieValue.equals(this.createKey(entry, context))) {
        return entry;
      }
    }

    return null;
  }

  public void logout(IHttpContext context) {
    Preconditions.checkNotNull(context);

    SessionEntry sessionEntry = this.getValidSessionEntry(this.getCookieValue(context), context);
    if (sessionEntry != null) {
      this.entries.remove(sessionEntry);
    }

    context.removeCookie(COOKIE_NAME);
  }

  public IPermissionUser getUser(IHttpContext context) {
    Preconditions.checkNotNull(context);

    SessionEntry sessionEntry = this.getValidSessionEntry(this.getCookieValue(context), context);

    return this.getUser(sessionEntry, context);
  }

  private IPermissionUser getUser(SessionEntry sessionEntry, IHttpContext context) {
    if (sessionEntry == null || context == null) {
      return null;
    }

    return CloudNet.getInstance().getPermissionManagement().getUser(UUID.fromString(sessionEntry.userUniqueId));
  }

  private String getCookieValue(IHttpContext context) {
    HttpCookie httpCookie = context.cookie(COOKIE_NAME);

    if (httpCookie != null) {
      return httpCookie.getValue();
    } else {
      return null;
    }
  }

  private String createKey(SessionEntry sessionEntry, IHttpContext context) {
    return Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(sessionEntry.creationTime +
      ":" +
      context.channel().clientAddress().getHost() +
      "#" +
      sessionEntry.uniqueId +
      "#" +
      sessionEntry.userUniqueId
    ));
  }

  public static class SessionEntry {

    private final long creationTime;
    private final String uniqueId;
    private final String userUniqueId;
    private long lastUsageMillis;

    public SessionEntry(long creationTime, long lastUsageMillis, String uniqueId, String userUniqueId) {
      this.creationTime = creationTime;
      this.lastUsageMillis = lastUsageMillis;
      this.uniqueId = uniqueId;
      this.userUniqueId = userUniqueId;
    }
  }
}
