package de.dytanic.cloudnet.driver.permission;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.encrypt.EncryptTo;
import lombok.Getter;

import java.lang.reflect.Type;
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

    @Getter
    protected final UUID uniqueId;

    @Getter
    protected final Collection<PermissionUserGroupInfo> groups;

    @Getter
    private String hashedPassword;

    public PermissionUser(UUID uniqueId, String name, String password, int potency)
    {
        this.uniqueId = uniqueId;
        this.name = name;
        this.hashedPassword = password == null ? null : Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password));
        this.potency = potency;
        this.groups = Iterables.newArrayList();
    }

    public void changePassword(String password)
    {
        this.hashedPassword = password == null ? null : Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password));
    }

    public boolean checkPassword(String password)
    {
        return this.hashedPassword != null && password != null && this.hashedPassword.equals(Base64.getEncoder().encodeToString(EncryptTo.encryptToSHA256(password)));
    }
}