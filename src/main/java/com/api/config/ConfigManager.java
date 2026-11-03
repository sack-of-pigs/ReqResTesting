package com.api.config;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

/**
 * Централизованный доступ к настройкам фреймворка.
 *
 * Порядок разрешения значения свойства (от высшего приоритета к низшему):
 *   1. System property           → -Dapi.key=... (удобно для локальных запусков и CI)
 *   2. Environment variable      → API_KEY=...     (стандарт для секретов в пайплайнах)
 *   3. Файл config.properties    → src/test/resources/config.properties
 *
 * Класс финальный и с приватным конструктором — это утилита, экземпляры не нужны.
 */

public final class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = loadProperties();

    private ConfigManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ---------------------------------------------------------------
    // Публичный API
    // ---------------------------------------------------------------

    public static String getBaseUri() {
        return resolve("base.uri");
    }

    public static String getApiKey() {
        return resolve("api.key");
    }

    // ---------------------------------------------------------------
    // Внутренняя механика
    // ---------------------------------------------------------------

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Файл '" + CONFIG_FILE + "' не найден в classpath. "
                                + "Убедитесь, что он лежит в src/test/resources. "
                                + "Скопируйте config.properties.example и заполните значения."
                );
            }
            props.load(input);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось прочитать '" + CONFIG_FILE + "'", e);
        }
        return props;
    }

    private static String resolve(String key) {
        // 1. System property (-Dapi.key=...)
        String sysProp = System.getProperty(key);
        if (isNotBlank(sysProp)) {
            return sysProp.trim();
        }

        // 2. Environment variable (api.key -> API_KEY)
        String envName = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envName);
        if (isNotBlank(envValue)) {
            return envValue.trim();
        }

        // 3. Файл config.properties
        String fileValue = PROPERTIES.getProperty(key);
        if (isNotBlank(fileValue)) {
            return fileValue.trim();
        }

        // Ничего не нашли — падаем громко и с понятным сообщением
        throw new IllegalStateException(
                String.format(
                        "Свойство '%s' не задано. Проверьте одно из мест: "
                                + "system property '-D%s=...', "
                                + "переменную окружения '%s' "
                                + "или ключ в %s.",
                        key, key, envName, CONFIG_FILE
                )
        );
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
