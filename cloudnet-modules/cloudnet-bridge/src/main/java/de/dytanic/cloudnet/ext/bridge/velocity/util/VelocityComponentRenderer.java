package de.dytanic.cloudnet.ext.bridge.velocity.util;

import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public final class VelocityComponentRenderer {

    private VelocityComponentRenderer() {
        throw new UnsupportedOperationException();
    }

    public static Component prefixed(@NotNull String text) {
        return raw(prefix() + text);
    }

    public static Component prefixedTranslation(@NotNull String translationKey) {
        return prefixedTranslation(translationKey, null);
    }

    public static Component prefixedTranslation(@NotNull String translationKey, @Nullable UnaryOperator<String> configurator) {
        String message = BridgeConfigurationProvider.load().getMessages().getOrDefault(translationKey, "null");
        if (configurator != null) {
            message = configurator.apply(message);
        }

        return raw(prefix() + message.replace('&', '§'));
    }

    public static Component rawTranslation(@NotNull String translationKey) {
        return prefixedTranslation(translationKey, null);
    }

    public static Component rawTranslation(@NotNull String translationKey, @Nullable UnaryOperator<String> configurator) {
        String message = BridgeConfigurationProvider.load().getMessages().getOrDefault(translationKey, "null");
        if (configurator != null) {
            message = configurator.apply(message);
        }

        return raw(message.replace('&', '§'));
    }

    public static Component raw(@NotNull String text) {
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    private static String prefix() {
        return BridgeConfigurationProvider.load().getPrefix().replace('&', '§');
    }
}
