package com.api.clients;

import com.api.core.specs.RequestSpecs;
import com.api.models.request.LoginRequest;
import com.api.models.request.RegisterRequest;
import com.api.models.response.LoginResponse;
import com.api.models.response.RegisterResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Клиент для эндпоинтов аутентификации ReqRes.
 *
 * Демо-эндпоинты /api/login и /api/register работают и без API-ключа,
 * но наша общая спецификация его добавляет — это не мешает и
 * сохраняет единообразие всех запросов фреймворка.
 */
public class AuthClient {

    private final RequestSpecification spec;

    public AuthClient() {
        this.spec = RequestSpecs.defaultRequestSpec();
    }

    public AuthClient(RequestSpecification spec) {
        this.spec = spec;
    }

    // ---------------------------------------------------------------
    // POST /api/login
    // ---------------------------------------------------------------

    /**
     * Успешный логин. ReqRes возвращает 200 и токен.
     *
     * Работающие демо-креды: eve.holt@reqres.in / cityslicka[reference:4].
     */
    public LoginResponse login(LoginRequest request) {
        return loginRaw(request)
                .then()
                .statusCode(200)
                .extract()
                .as(LoginResponse.class);
    }

    /**
     * «Сырой» логин — для негативных тестов (400/401)
     * и проверки заголовков ответа.
     */
    public Response loginRaw(LoginRequest request) {
        return given()
                .spec(spec)
                .body(request)
                .when()
                .post("/api/login");
    }

    // ---------------------------------------------------------------
    // POST /api/register
    // ---------------------------------------------------------------

    /**
     * Успешная регистрация. ReqRes возвращает 200, id и token.
     *
     * Работающие демо-креды: eve.holt@reqres.in / pistol[reference:5].
     */
    public RegisterResponse register(RegisterRequest request) {
        return registerRaw(request)
                .then()
                .statusCode(200)
                .extract()
                .as(RegisterResponse.class);
    }

    /**
     * «Сырая» регистрация — для негативных тестов.
     */
    public Response registerRaw(RegisterRequest request) {
        return given()
                .spec(spec)
                .body(request)
                .when()
                .post("/api/register");
    }
}
