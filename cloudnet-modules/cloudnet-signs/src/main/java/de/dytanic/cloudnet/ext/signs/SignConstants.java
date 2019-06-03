package de.dytanic.cloudnet.ext.signs;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;

public final class SignConstants {

    private SignConstants()
    {
        throw new UnsupportedOperationException();
    }

    public static final Type COLLECTION_SIGNS = new TypeToken<Collection<Sign>>() {
    }.getType();

    public static final String
        SIGN_CLUSTER_CHANNEL_NAME = "cloudnet_cluster_signs_channel",
        SIGN_CHANNEL_NAME = "cloudnet_signs_channel",
        SIGN_CHANNEL_SYNC_CHANNEL_PROPERTY = "cloudnet_signs_channel",
        SIGN_CHANNEL_SYNC_ID_GET_SIGNS_COLLECTION_PROPERTY = "signs_get_signs_collection",
        SIGN_CHANNEL_SYNC_ID_GET_SIGNS_CONFIGURATION_PROPERTY = "signs_get_signs_configuration",
        SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION = "update_sign_configuration",
        SIGN_CHANNEL_ADD_SIGN_MESSAGE = "add_sign",
        SIGN_CHANNEL_REMOVE_SIGN_MESSAGE = "remove_sign";

}