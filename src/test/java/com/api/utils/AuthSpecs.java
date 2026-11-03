package com.api.utils;
import com.api.config.ConfigManager;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public class AuthSpecs {

    // Спецификация без ключа (или с пустым)
    public static RequestSpecification noAuthSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(ConfigManager.getBaseUri())
                .setContentType(ContentType.JSON)
                .build();
    }

    // Спецификация с заведомо неверным ключом
    public static RequestSpecification invalidKeySpec() {
        return new RequestSpecBuilder()
                .setBaseUri(ConfigManager.getBaseUri())
                .setContentType(ContentType.JSON)
                .addHeader("x-api-key", "INVALID_KEY_12345")
                .build();
    }

    // Спецификация с ключом, но неверным Content-Type
    public static RequestSpecification wrongContentTypeSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(ConfigManager.getBaseUri())
                .setContentType(ContentType.TEXT)
                .addHeader("x-api-key", ConfigManager.getApiKey())
                .build();
    }
}