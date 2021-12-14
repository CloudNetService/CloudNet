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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import de.dytanic.cloudnet.driver.permission.PermissionUser;
import de.dytanic.cloudnet.http.ticket.WebSocketTicketManager;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.PrematureJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.sql.Date;
import java.util.Base64;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class V2HttpAuthentication {

  protected static final String ISSUER = "CloudNet " + CloudNet.getInstance().getComponentName();

  protected static final Key SIGN_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  protected static final JwtParser PARSER = Jwts.parserBuilder().setSigningKey(SIGN_KEY).requireIssuer(ISSUER).build();

  protected static final Pattern BASIC_LOGIN_PATTERN = Pattern.compile("Basic ([a-zA-Z0-9=]+)$");
  protected static final Pattern BEARER_LOGIN_PATTERN = Pattern.compile("Bearer ([a-zA-Z0-9-_.]+)$");

  protected static final LoginResult<HttpSession> ERROR_HANDLING_BEARER_LOGIN = LoginResult.failure(
    "Unable to process bearer login");
  protected static final LoginResult<HttpSession> ERROR_HANDLING_BEARER_LOGIN_USER_GONE = LoginResult.failure(
    "Unable to process bearer login: user gone");
  protected static final LoginResult<PermissionUser> ERROR_HANDLING_BASIC_LOGIN = LoginResult.failure(
    "No matching user for provided basic login credentials");

  protected static final Logger LOGGER = LogManager.getLogger(V2HttpAuthentication.class);

  protected final WebSocketTicketManager webSocketTicketManager;
  protected final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();

  public V2HttpAuthentication() {
    this(WebSocketTicketManager.memoryCached());
  }

  public V2HttpAuthentication(WebSocketTicketManager webSocketTicketManager) {
    this.webSocketTicketManager = webSocketTicketManager;
  }

  public @NotNull String createJwt(@NotNull PermissionUser subject, long sessionTimeMillis) {
    var session = this.getSessions().computeIfAbsent(subject.getUniqueId().toString(),
      userUniqueId -> new DefaultHttpSession(System.currentTimeMillis() + sessionTimeMillis, subject.getUniqueId()));
    return this.generateJwt(subject, session);
  }

  public @NotNull LoginResult<PermissionUser> handleBasicLoginRequest(@NotNull IHttpRequest request) {
    var authenticationHeader = request.header("Authorization");
    if (authenticationHeader == null) {
      return LoginResult.undefinedFailure();
    }

    var matcher = BASIC_LOGIN_PATTERN.matcher(authenticationHeader);
    if (matcher.matches()) {
      var credentials = new String(Base64.getDecoder().decode(matcher.group(1)), StandardCharsets.UTF_8)
        .split(":");
      if (credentials.length == 2) {
        var users = CloudNetDriver.getInstance().getPermissionManagement().getUsersByName(credentials[0]);
        for (var user : users) {
          if (user.checkPassword(credentials[1])) {
            return LoginResult.success(user);
          }
        }
      }
      return ERROR_HANDLING_BASIC_LOGIN;
    }

    return LoginResult.undefinedFailure();
  }

  public @NotNull LoginResult<HttpSession> handleBearerLoginRequest(@NotNull IHttpRequest request) {
    var authenticationHeader = request.header("Authorization");
    if (authenticationHeader == null) {
      return LoginResult.undefinedFailure();
    }

    var matcher = BEARER_LOGIN_PATTERN.matcher(authenticationHeader);
    if (matcher.matches()) {
      try {
        var jws = PARSER.parseClaimsJws(matcher.group(1));
        var session = this.getSessionById(jws.getBody().getId());
        if (session != null) {
          var user = session.getUser();
          if (user == null) {
            // the user associated with the session no longer exists
            this.sessions.remove(session.getUserId().toString());
            return ERROR_HANDLING_BEARER_LOGIN_USER_GONE;
          }
          // ensure that the user is the owner of the session
          var userUniqueId = UUID.fromString(jws.getBody().get("uniqueId", String.class));
          if (user.getUniqueId().equals(userUniqueId)) {
            return LoginResult.success(session);
          }
        }
      } catch (JwtException | IllegalArgumentException exception) {
        LOGGER.log(Level.FINE, "Exception while handling bearer auth", exception);
        // the key is not yet usable or too old
        if (exception instanceof PrematureJwtException || exception instanceof ExpiredJwtException) {
          return LoginResult.failure(exception.getMessage());
        }
      }
      return ERROR_HANDLING_BEARER_LOGIN;
    }

    return LoginResult.undefinedFailure();
  }

  public boolean expireSession(@NotNull IHttpRequest request) {
    var session = this.handleBearerLoginRequest(request);
    if (session.isSuccess()) {
      return this.expireSession(session.getResult());
    } else {
      return false;
    }
  }

  public boolean expireSession(@NotNull HttpSession session) {
    return this.sessions.remove(session.getUser().getUniqueId().toString()) != null;
  }

  public @NotNull LoginResult<Pair<HttpSession, String>> refreshJwt(@NotNull IHttpRequest request, long lifetime) {
    var session = this.handleBearerLoginRequest(request);
    if (session.isSuccess()) {
      var httpSession = session.getResult();
      return LoginResult.success(new Pair<>(httpSession, this.refreshJwt(httpSession, lifetime)));
    } else {
      return LoginResult.undefinedFailure();
    }
  }

  public @NotNull String refreshJwt(@NotNull HttpSession session, long lifetime) {
    session.refreshFor(lifetime);
    return this.generateJwt(session.getUser(), session);
  }

  protected @Nullable HttpSession getSessionById(@NotNull String id) {
    for (var session : this.getSessions().values()) {
      if (session.getUniqueId().equals(id)) {
        return session;
      }
    }
    return null;
  }

  protected @NotNull String generateJwt(@NotNull PermissionUser subject, @NotNull HttpSession session) {
    return Jwts.builder()
      .setIssuer(ISSUER)
      .signWith(SIGN_KEY)
      .setSubject(subject.getName())
      .setId(session.getUniqueId())
      .setIssuedAt(Calendar.getInstance().getTime())
      .claim("uniqueId", subject.getUniqueId())
      .setExpiration(new Date(session.getExpireTime()))
      .compact();
  }

  protected void cleanup() {
    for (var entry : this.sessions.entrySet()) {
      if (entry.getValue().getExpireTime() <= System.currentTimeMillis()) {
        this.sessions.remove(entry.getKey());
      }
    }
  }

  public @NotNull Map<String, HttpSession> getSessions() {
    this.cleanup();
    return this.sessions;
  }

  public @NotNull WebSocketTicketManager getWebSocketTicketManager() {
    return this.webSocketTicketManager;
  }

  public static class LoginResult<T> {

    private static final LoginResult<?> UNDEFINED_RESULT = LoginResult.failure(null);

    private final T result;
    private final String errorMessage;

    protected LoginResult(T result, String errorMessage) {
      this.result = result;
      this.errorMessage = errorMessage;
    }

    @SuppressWarnings("unchecked")
    public static <T> LoginResult<T> undefinedFailure() {
      return (LoginResult<T>) UNDEFINED_RESULT;
    }

    public static <T> LoginResult<T> success(@NotNull T result) {
      return new LoginResult<>(result, null);
    }

    public static <T> LoginResult<T> failure(@Nullable String errorMessage) {
      return new LoginResult<>(null, errorMessage);
    }

    public boolean isSuccess() {
      return this.result != null;
    }

    public boolean isFailure() {
      return this.result == null;
    }

    public boolean hasErrorMessage() {
      return this.errorMessage != null;
    }

    public @Nullable String getErrorMessage() {
      return this.errorMessage;
    }

    public @Nullable T getResult() {
      return this.result;
    }
  }
}
