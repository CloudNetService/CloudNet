package de.dytanic.cloudnet.http.v2;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.IHttpRequest;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.PrematureJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.sql.Date;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    protected static final LoginResult<IPermissionUser> ERROR_HANDLING_BASIC_LOGIN = LoginResult.failure(
            "No matching user for provided basic login credentials");

    protected final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();

    public @NotNull String createJwt(@NotNull IPermissionUser subject, long sessionTimeMillis) {
        HttpSession session = this.getSessions().computeIfAbsent(subject.getUniqueId().toString(),
                userUniqueId -> new DefaultHttpSession(System.currentTimeMillis() + sessionTimeMillis, subject.getUniqueId()));
        return this.generateJwt(subject, session);
    }

    public @NotNull LoginResult<IPermissionUser> handleBasicLoginRequest(@NotNull IHttpRequest request) {
        String authenticationHeader = request.header("Authorization");
        if (authenticationHeader == null) {
            return LoginResult.undefinedFailure();
        }

        Matcher matcher = BASIC_LOGIN_PATTERN.matcher(authenticationHeader);
        if (matcher.matches()) {
            String[] credentials = new String(Base64.getDecoder().decode(matcher.group(1)), StandardCharsets.UTF_8).split(":");
            if (credentials.length == 2) {
                List<IPermissionUser> users = CloudNetDriver.getInstance().getPermissionManagement().getUsers(credentials[0]);
                for (IPermissionUser user : users) {
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
        String authenticationHeader = request.header("Authorization");
        if (authenticationHeader == null) {
            return LoginResult.undefinedFailure();
        }

        Matcher matcher = BEARER_LOGIN_PATTERN.matcher(authenticationHeader);
        if (matcher.matches()) {
            try {
                Jws<Claims> jws = PARSER.parseClaimsJws(matcher.group(1));
                HttpSession session = this.getSessionById(jws.getBody().getId());
                if (session != null) {
                    IPermissionUser user = session.getUser();
                    if (user == null) {
                        // the user associated with the session no longer exists
                        this.sessions.remove(session.getUserId().toString());
                        return ERROR_HANDLING_BEARER_LOGIN_USER_GONE;
                    }
                    // ensure that the user is the owner of the session
                    UUID userUniqueId = UUID.fromString(jws.getBody().get("uniqueId", String.class));
                    if (user.getUniqueId().equals(userUniqueId)) {
                        return LoginResult.success(session);
                    }
                }
            } catch (JwtException | IllegalArgumentException exception) {
                CloudNet.getInstance().getLogger().debug("Exception while handling bearer auth", exception);
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
        LoginResult<HttpSession> session = this.handleBearerLoginRequest(request);
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
        LoginResult<HttpSession> session = this.handleBearerLoginRequest(request);
        if (session.isSuccess()) {
            HttpSession httpSession = session.getResult();
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
        for (HttpSession session : this.getSessions().values()) {
            if (session.getUniqueId().equals(id)) {
                return session;
            }
        }
        return null;
    }

    protected @NotNull String generateJwt(@NotNull IPermissionUser subject, @NotNull HttpSession session) {
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
        for (Map.Entry<String, HttpSession> entry : this.sessions.entrySet()) {
            if (entry.getValue().getExpireTime() <= System.currentTimeMillis()) {
                this.sessions.remove(entry.getKey());
            }
        }
    }

    public @NotNull Map<String, HttpSession> getSessions() {
        this.cleanup();
        return this.sessions;
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

        public String getErrorMessage() {
            return this.errorMessage;
        }

        public T getResult() {
            return this.result;
        }
    }
}
