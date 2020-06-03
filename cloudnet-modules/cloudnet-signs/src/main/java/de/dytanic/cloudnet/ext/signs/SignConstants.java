package de.dytanic.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;

public final class SignConstants {

    public static final Type COLLECTION_SIGNS = new TypeToken<Collection<Sign>>() {
    }.getType();
    public static final String
            SIGN_CLUSTER_CHANNEL_NAME = "cloudnet_cluster_signs_channel",
            SIGN_CHANNEL_NAME = "cloudnet_signs_channel",
            SIGN_CHANNEL_GET_SIGNS = "signs_get_signs_collection",
            SIGN_CHANNEL_GET_SIGNS_CONFIGURATION = "signs_get_signs_configuration",
            SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION = "update_sign_configuration",
            SIGN_CHANNEL_ADD_SIGN_MESSAGE = "add_sign",
            SIGN_CHANNEL_REMOVE_SIGN_MESSAGE = "remove_sign";

    private SignConstants() {
        throw new UnsupportedOperationException();
    }

}