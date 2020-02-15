package de.dytanic.cloudnet.driver.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

/**
 * The default implementation of the IPermissionUser class. This class should use if you want to
 * add new PermissionUsers into the IPermissionManagement implementation
 */
public class PermissionUser extends AbstractPermissible implements IPermissionUser {

    /**
     * The Gson TypeToken result of the PermissionUser class
     */
    public static final Type TYPE = new TypeToken<PermissionUser>() {
    }.getType();

    protected final UUID uniqueId;

    protected final Collection<PermissionUserGroupInfo> groups;

    private String hashedPassword;

    public PermissionUser(@NotNull UUID uniqueId, @NotNull String name, @Nullable String password, int potency) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.hashedPassword = password == null ? null : Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password));
        this.potency = potency;
        this.groups = new ArrayList<>();
    }

    public void changePassword(String password) {
        this.hashedPassword = password == null ? null : Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password));
    }

    public boolean checkPassword(String password) {
        return this.hashedPassword != null && password != null && this.hashedPassword.equals(Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password)));
    }

    @NotNull
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public Collection<PermissionUserGroupInfo> getGroups() {
        return this.groups;
    }

    public String getHashedPassword() {
        return this.hashedPassword;
    }
}