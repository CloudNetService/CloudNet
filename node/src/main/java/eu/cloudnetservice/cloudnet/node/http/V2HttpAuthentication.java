/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.http;

import eu.cloudnetservice.cloudnet.common.collection.Pair;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.CloudNetDriver;
import eu.cloudnetservice.cloudnet.driver.network.http.HttpRequest;
import eu.cloudnetservice.cloudnet.driver.permission.PermissionUser;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.http.ticket.WebSocketTicketManager;
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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class V2HttpAuthentication {

  protected static final Logger LOGGER = LogManager.logger(V2HttpAuthentication.class);
  protected static final String ISSUER = "CloudNet " + CloudNet.instance().componentName();

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

  protected final WebSocketTicketManager webSocketTicketManager;
  protected final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();

  public V2HttpAuthentication() {
    this(WebSocketTicketManager.memoryCached());
  }

  public V2HttpAuthentication(@NonNull WebSocketTicketManager webSocketTicketManager) {
    this.webSocketTicketManager = webSocketTicketManager;
  }

  public @NonNull String createJwt(@NonNull PermissionUser subject, long sessionTimeMillis) {
    var session = this.sessions().computeIfAbsent(
      subject.uniqueId().toString(),
      userUniqueId -> new DefaultHttpSession(System.currentTimeMillis() + sessionTimeMillis, subject.uniqueId()));
    return this.generateJwt(subject, session);
  }

  public @NonNull LoginResult<PermissionUser> handleBasicLoginRequest(@NonNull HttpRequest request) {
    var authenticationHeader = request.header("Authorization");
    if (authenticationHeader == null) {
      return LoginResult.undefinedFailure();
    }

    var matcher = BASIC_LOGIN_PATTERN.matcher(authenticationHeader);
    if (matcher.matches()) {
      var auth = new String(Base64.getDecoder().decode(matcher.group(1)), StandardCharsets.UTF_8).split(":");
      if (auth.length == 2) {
        var users = CloudNetDriver.instance().permissionManagement().usersByName(auth[0]);
        for (var user : users) {
          if (user.checkPassword(auth[1])) {
            return LoginResult.success(user);
          }
        }
      }
      return ERROR_HANDLING_BASIC_LOGIN;
    }

    return LoginResult.undefinedFailure();
  }

  public @NonNull LoginResult<HttpSession> handleBearerLoginRequest(@NonNull HttpRequest request) {
    var authenticationHeader = request.header("Authorization");
    if (authenticationHeader == null) {
      return LoginResult.undefinedFailure();
    }

    var matcher = BEARER_LOGIN_PATTERN.matcher(authenticationHeader);
    if (matcher.matches()) {
      try {
        var jws = PARSER.parseClaimsJws(matcher.group(1));
        var session = this.sessionById(jws.getBody().getId());
        if (session != null) {
          var user = session.user();
          if (user == null) {
            // the user associated with the session no longer exists
            this.sessions.remove(session.userId().toString());
            return ERROR_HANDLING_BEARER_LOGIN_USER_GONE;
          }
          // ensure that the user is the owner of the session
          var userUniqueId = UUID.fromString(jws.getBody().get("uniqueId", String.class));
          if (user.uniqueId().equals(userUniqueId)) {
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

  public boolean expireSession(@NonNull HttpRequest request) {
    var session = this.handleBearerLoginRequest(request);
    if (session.succeeded()) {
      return this.expireSession(session.result());
    } else {
      return false;
    }
  }

  public boolean expireSession(@NonNull HttpSession session) {
    return this.sessions.remove(session.user().uniqueId().toString()) != null;
  }

  public @NonNull LoginResult<Pair<HttpSession, String>> refreshJwt(@NonNull HttpRequest request, long lifetime) {
    var session = this.handleBearerLoginRequest(request);
    if (session.succeeded()) {
      var httpSession = session.result();
      return LoginResult.success(new Pair<>(httpSession, this.refreshJwt(httpSession, lifetime)));
    } else {
      return LoginResult.undefinedFailure();
    }
  }

  public @NonNull String refreshJwt(@NonNull HttpSession session, long lifetime) {
    session.refreshFor(lifetime);
    return this.generateJwt(session.user(), session);
  }

  protected @Nullable HttpSession sessionById(@NonNull String id) {
    for (var session : this.sessions().values()) {
      if (session.uniqueId().equals(id)) {
        return session;
      }
    }
    return null;
  }

  protected @NonNull String generateJwt(@NonNull PermissionUser subject, @NonNull HttpSession session) {
    return Jwts.builder()
      .setIssuer(ISSUER)
      .signWith(SIGN_KEY)
      .setSubject(subject.name())
      .setId(session.uniqueId())
      .setIssuedAt(Calendar.getInstance().getTime())
      .claim("uniqueId", subject.uniqueId())
      .setExpiration(new Date(session.expireTime()))
      .compact();
  }

  protected void cleanup() {
    for (var entry : this.sessions.entrySet()) {
      if (entry.getValue().expireTime() <= System.currentTimeMillis()) {
        this.sessions.remove(entry.getKey());
      }
    }
  }

  public @NonNull Map<String, HttpSession> sessions() {
    this.cleanup();
    return this.sessions;
  }

  public @NonNull WebSocketTicketManager webSocketTicketManager() {
    return this.webSocketTicketManager;
  }

  public record LoginResult<T>(@UnknownNullability T result, @UnknownNullability String errorMessage) {

    private static final LoginResult<?> UNDEFINED_RESULT = LoginResult.failure(null);

    @SuppressWarnings("unchecked")
    public static <T> @NonNull LoginResult<T> undefinedFailure() {
      return (LoginResult<T>) UNDEFINED_RESULT;
    }

    public static <T> @NonNull LoginResult<T> success(@NonNull T result) {
      return new LoginResult<>(result, null);
    }

    public static <T> @NonNull LoginResult<T> failure(@Nullable String errorMessage) {
      return new LoginResult<>(null, errorMessage);
    }

    public boolean succeeded() {
      return this.result != null;
    }

    public boolean failed() {
      return this.result == null;
    }

    public boolean hasErrorMessage() {
      return this.errorMessage != null;
    }
  }
}
