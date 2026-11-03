package com.api.clients;
import com.api.core.specs.RequestSpecs;
import com.api.models.request.CreateUserRequest;
import com.api.models.response.CreateUserResponse;
import com.api.models.response.UpdateUserResponse;
import com.api.models.response.UserResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Клиент для работы с эндпоинтами /api/users.
 *
 * Инкапсулирует все технические детали HTTP-запросов.
 * Тесты вызывают методы этого класса и работают с типизированными
 * объектами (UserResponse, CreateUserResponse), а не с сырым Response.
 */
public class UserClient {

    private final RequestSpecification spec;

    /**
     * По умолчанию использует общую спецификацию фреймворка
     * (базовый URI + заголовок x-api-key).
     */
    public UserClient() {
        this.spec = RequestSpecs.defaultRequestSpec();
    }

    /**
     * Конструктор для случаев, когда нужна кастомная спецификация
     * (например, без ключа для проверки 401, или с другими таймаутами).
     */
    public UserClient(RequestSpecification spec) {
        this.spec = spec;
    }

    // ---------------------------------------------------------------
    // GET /api/users/{id}
    // ---------------------------------------------------------------

    /**
     * Возвращает «сырой» ответ. Полезно, когда в тесте нужно
     * проверить статус-код или заголовки, а не только тело.
     *
     * Для несуществующего ID (например, 23) ReqRes вернёт 404
     * с пустым JSON-объектом {} — это НЕ ошибка клиента,
     * а валидный сценарий для проверки обработки 404.
     */
    public Response getUserByIdRaw(int userId) {
        return given()
                .spec(spec)
                .when()
                .get("/api/users/{id}", userId);
    }

    /**
     * Типизированный вариант: сразу десериализует ответ в UserResponse.
     *
     * ВНИМАНИЕ: если пользователь не найден (404), поля внутри
     * UserResponse.data будут null. Тест должен сначала проверить
     * статус-код, а потом обращаться к data.
     */
    public UserResponse getUserById(int userId) {
        return getUserByIdRaw(userId)
                .then()
                .statusCode(200)
                .extract()
                .as(UserResponse.class);
    }

    // ---------------------------------------------------------------
    // POST /api/users
    // ---------------------------------------------------------------

    /**
     * Создаёт пользователя и возвращает типизированный ответ.
     *
     * Ответ на POST отличается по форме от GET /users/{id}:
     *   - нет обёртки "data"
     *   - поле "id" — строка, а не число
     *   - добавлено "createdAt"
     * Поэтому используется отдельная модель CreateUserResponse.
     */
    public CreateUserResponse createUser(CreateUserRequest request) {
        return given()
                .spec(spec)
                .body(request)
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .as(CreateUserResponse.class);
    }

    /**
     * «Сырой» вариант создания — если в тесте нужно проверить
     * заголовки ответа, время ответа и т.п.
     */
    public Response createUserRaw(CreateUserRequest request) {
        return given()
                .spec(spec)
                .body(request)
                .when()
                .post("/api/users");
    }

    // ---------------------------------------------------------------
    // DELETE /api/users/{id}
    // ---------------------------------------------------------------

    /**
     * Удаляет пользователя.
     *
     * ReqRes возвращает 204 No Content с пустым телом.
     * Поэтому метод возвращает void — десериализовать нечего.
     * Если нужен доступ к статус-коду или заголовкам,
     * используйте deleteUserRaw().
     */
    public void deleteUser(int userId) {
        deleteUserRaw(userId)
                .then()
                .statusCode(204);
    }

    /**
     * «Сырой» вариант удаления — для проверки статус-кода,
     * заголовков или времени ответа.
     */
    public Response deleteUserRaw(int userId) {
        return given()
                .spec(spec)
                .when()
                .delete("/api/users/{id}", userId);
    }

    // в UserClient.java

    /**
     * Возвращает «сырой» ответ со списком пользователей.
     * Query-параметры page и per_page опциональны:
     *   getUsersRaw(2, 6)  → GET /api/users?page=2&per_page=6
     *   getUsersRaw(1, 3)  → GET /api/users?page=1&per_page=3
     */
    public Response getUsersRaw(int page, int perPage) {
        return given()
                .spec(spec)
                .queryParam("page", page)
                .queryParam("per_page", perPage)
                .when()
                .get("/api/users");
    }

    public Response getUsersRaw(int page, int perPage, int delaySeconds) {
        return given()
                .spec(spec)
                .queryParam("page", page)
                .queryParam("per_page", perPage)
                .queryParam("delay", delaySeconds)
                .when()
                .get("/api/users");
    }

    /**
     * «Сырой» GET по строковому ID — для проверки невалидных форматов
     * (например, "abc", "1.5", "-1").
     */
    public Response getUserByStringIdRaw(String userId) {
        return given()
                .spec(spec)
                .when()
                .get("/api/users/{id}", userId);
    }

    // ---------------------------------------------------------------
    // PUT /api/users/{id}
    // ---------------------------------------------------------------

    public UpdateUserResponse updateUser(int userId, CreateUserRequest request) {
        return given()
                .spec(spec)
                .body(request)
                .when()
                .put("/api/users/{id}", userId)
                .then()
                .statusCode(200)
                .extract()
                .as(UpdateUserResponse.class);
    }

    public Response updateUserRaw(int userId, CreateUserRequest request) {
        return given()
                .spec(spec)
                .body(request)
                .when()
                .put("/api/users/{id}", userId);
    }

    // ---------------------------------------------------------------
    // PATCH /api/users/{id}
    // ---------------------------------------------------------------

    public UpdateUserResponse patchUser(int userId, CreateUserRequest request) {
        return given()
                .spec(spec)
                .body(request)
                .when()
                .patch("/api/users/{id}", userId)
                .then()
                .statusCode(200)
                .extract()
                .as(UpdateUserResponse.class);
    }
}
