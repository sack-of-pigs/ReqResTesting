package com.api.config;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties PROPERTIES = new Properties();

    // Статический блок загрузки файла (теперь безопасный)
    static {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                PROPERTIES.load(input);
            } else {
                // Если файла нет (например, в CI), просто выводим предупреждение, но не падаем
                System.out.println("WARN: Файл " + CONFIG_FILE + " не найден в classpath. Будут использованы переменные окружения.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать '" + CONFIG_FILE + "'", e);
        }
    }

    private ConfigManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getBaseUri() {
        return resolve("base.uri", "https://reqres.in"); // Передаем дефолтное значение
    }

    public static String getApiKey() {
        return resolve("api.key", null);
    }

    private static String resolve(String key, String defaultValue) {
        // 1. System property (-Dapi.key=...)
        String sysProp = System.getProperty(key);
        if (isNotBlank(sysProp)) return sysProp.trim();

        // 2. Environment variable (api.key -> API_KEY)
        String envName = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envName);
        if (isNotBlank(envValue)) return envValue.trim();

        // 3. Файл config.properties
        String fileValue = PROPERTIES.getProperty(key);
        if (isNotBlank(fileValue)) return fileValue.trim();

        // 4. Если ничего не нашли, но есть дефолтное значение (для base.uri)
        if (defaultValue != null) {
            return defaultValue;
        }

        throw new IllegalStateException(
                String.format("Свойство '%s' не задано. Проверьте system property, переменную окружения '%s' или файл %s.",
                        key, envName, CONFIG_FILE)
        );
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
