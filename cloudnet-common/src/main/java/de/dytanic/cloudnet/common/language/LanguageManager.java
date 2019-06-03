package de.dytanic.cloudnet.common.language;

import de.dytanic.cloudnet.common.Properties;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The LanguageManager is a static service, which handles messages from different languages, which are registered or loaded there
 */
public final class LanguageManager {

    private LanguageManager()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * The current language, which the getMessage() method will the message return
     */
    @Getter
    @Setter
    private static volatile String language;

    private static final Map<String, Properties> LANGUAGE_CACHE = new HashMap<>();

    /**
     * Resolve and returns the following message in the language which is currently set as member "language"
     *
     * @param property the following message property, which should sort out
     * @return the message which is defined in language cache or a fallback message like "MESSAGE OR LANGUAGE NOT FOUND!"
     */
    public static String getMessage(String property)
    {
        if (language == null || !LANGUAGE_CACHE.containsKey(language)) return "MESSAGE OR LANGUAGE NOT FOUND!";

        return LANGUAGE_CACHE.get(language).get(property);
    }

    /**
     * Add a new language properties, which can resolve with getMessage() in the configured language.
     *
     * @param language   the language, which should append
     * @param properties the properties which will add in the language as parameter
     */
    public static void addLanguageFile(String language, Properties properties)
    {
        if (language == null || properties == null) return;

        if (LANGUAGE_CACHE.containsKey(language))
            LANGUAGE_CACHE.get(language).putAll(properties);
        else
            LANGUAGE_CACHE.put(language, properties);
    }

    /**
     * Add a new language properties, which can resolve with getMessage() in the configured language.
     *
     * @param language the language, which should append
     * @param file     the properties which will add in the language as parameter
     */
    public static void addLanguageFile(String language, File file)
    {
        addLanguageFile(language, file.toPath());
    }

    /**
     * Add a new language properties, which can resolve with getMessage() in the configured language.
     *
     * @param language the language, which should append
     * @param file     the properties which will add in the language as parameter
     */
    public static void addLanguageFile(String language, Path file)
    {
        try (InputStream inputStream = new FileInputStream(file.toFile()))
        {
            addLanguageFile(language, inputStream);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Add a new language properties, which can resolve with getMessage() in the configured language.
     *
     * @param language    the language, which should append
     * @param inputStream the properties which will add in the language as parameter
     */
    public static void addLanguageFile(String language, InputStream inputStream)
    {
        Properties properties = new Properties();

        try
        {
            properties.load(inputStream);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        addLanguageFile(language, properties);
    }
}