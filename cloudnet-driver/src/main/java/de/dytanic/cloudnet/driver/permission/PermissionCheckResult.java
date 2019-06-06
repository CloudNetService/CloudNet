package de.dytanic.cloudnet.driver.permission;

/**
 * A response, if a permissible has to check his permission that contains and allow and element
 */
public enum PermissionCheckResult {

    /**
     * The permissible has the following permission or the privileges to has the permission
     */
    ALLOWED(true),

    /**
     * The following permission is not defined or the potency from permissible or from the defined permission is not high enough
     */
    DENIED(false),

    /**
     * The following permission is set on permissible, but the potency is set to -1 and doesn't allow to has the permission
     */
    FORBIDDEN(false);

    private final boolean value;

    private PermissionCheckResult(boolean value) {
        this.value = value;
    }

    /**
     * Returns the result as boolean
     *
     * @return the result as boolean
     */
    public boolean asBoolean() {
        return value;
    }
}