package de.dytanic.cloudnet.http.v2;

import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface HttpSession {

    long getExpireTime();

    long refreshFor(long liveMillis);

    @NotNull String getUniqueId();

    @NotNull UUID getUserId();

    IPermissionUser getUser();

    <T> T getProperty(@NotNull String key);

    <T> T getProperty(@NotNull String key, @Nullable T def);

    @NotNull HttpSession setProperty(@NotNull String key, @NotNull Object value);

    @NotNull HttpSession removeProperty(@NotNull String key);

    boolean hasProperty(@NotNull String key);

    @NotNull Map<String, Object> getProperties();
}
