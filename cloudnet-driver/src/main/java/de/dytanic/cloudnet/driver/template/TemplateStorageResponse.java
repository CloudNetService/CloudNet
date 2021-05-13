package de.dytanic.cloudnet.driver.template;

public enum TemplateStorageResponse {

    SUCCESS,
    FAILED,
    TEMPLATE_STORAGE_NOT_FOUND,
    EXCEPTION;

    public static TemplateStorageResponse of(boolean success) {
        return success ? SUCCESS : FAILED;
    }

}
