package com.api.core.specs;

import com.api.config.ConfigManager;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

/**
 * Переиспользуемые спецификации REST Assured.
 *
 * Спецификации — это шаблоны, которые применяются к каждому запросу:
 *   - базовый URI (из ConfigManager)
 *   - Content-Type и Accept
 *   - заголовок авторизации x-api-key
 *
 * Создаются ОДИН раз при первой загрузке класса и переиспользуются.
 * REST Assured не мутирует спецификацию при применении через .spec(),
 * поэтому кэширование безопасно и экономит аллокации в каждом тесте.
 */
public final class RequestSpecs {

    private static final RequestSpecification DEFAULT_REQUEST_SPEC = buildDefaultRequestSpec();
    private static final ResponseSpecification DEFAULT_RESPONSE_SPEC = buildDefaultResponseSpec();

    private RequestSpecs() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ---------------------------------------------------------------
    // Публичный API
    // ---------------------------------------------------------------

    /**
     * Базовая спецификация для всех запросов.
     * Уже содержит ключ авторизации и правильные заголовки.
     */
    public static RequestSpecification defaultRequestSpec() {
        return DEFAULT_REQUEST_SPEC;
    }

    /**
     * Базовая спецификация для проверок ответа.
     * Пока минимальная — расширяйте по мере роста фреймворка.
     */
    public static ResponseSpecification defaultResponseSpec() {
        return DEFAULT_RESPONSE_SPEC;
    }

    // ---------------------------------------------------------------
    // Строители спецификаций
    // ---------------------------------------------------------------

    private static RequestSpecification buildDefaultRequestSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(ConfigManager.getBaseUri())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("x-api-key", ConfigManager.getApiKey())
                .build();
    }

    private static ResponseSpecification buildDefaultResponseSpec() {
        return new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();
    }
}
